#!/usr/bin/env python3
"""
string_generator.py

Analyses a Java string validator method and generates valid example strings.
Uses symbolic constraint solving where possible; falls back to an LLM only
when the method contains constructs that cannot be reduced statically.

Usage:
    python string_generator.py <java_file> <ClassName.methodName> [--count N] [--verbose]

Examples:
    python string_generator.py Validators.java Validators.isValidEmail --count 10
    python string_generator.py Validators.java Validators.isPalindrome -n 5 --verbose
"""

from __future__ import annotations
import sys
import argparse
import subprocess
import textwrap


# ── Dependency bootstrap ──────────────────────────────────────────────────────

def ensure_dependencies():
    packages = ["javalang", "anthropic", "exrex"]
    for pkg in packages:
        try:
            __import__(pkg)
        except ImportError:
            print(f"Installing {pkg}...", file=sys.stderr)
            subprocess.check_call(
                [sys.executable, "-m", "pip", "install", pkg, "--quiet",
                 "--break-system-packages"],
                stderr=subprocess.DEVNULL
            )

ensure_dependencies()

import javalang
import javalang.tree as jt

from constraints import OpaqueConstraint, FalseConstraint
from extractor import ConstraintExtractor
from solver import generate as symbolic_generate
from llm_fallback import generate_with_llm


# ── Java source loading ───────────────────────────────────────────────────────

def load_source(path: str) -> str:
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError:
        raise SystemExit(f"Error: file not found: {path}")
    except IOError as e:
        raise SystemExit(f"Error reading file: {e}")


def parse_and_find_method(source: str, class_name: str,
                           method_name: str) -> jt.MethodDeclaration:
    try:
        tree = javalang.parse.parse(source)
    except javalang.parser.JavaSyntaxError as e:
        raise SystemExit(f"Java syntax error: {e}")

    target_class = None
    for _, node in tree.filter(jt.ClassDeclaration):
        if node.name == class_name:
            target_class = node
            break

    if target_class is None:
        available = [n.name for _, n in tree.filter(jt.ClassDeclaration)]
        raise SystemExit(
            f"Class '{class_name}' not found. "
            f"Available: {', '.join(available) or '(none)'}"
        )

    for method in target_class.methods:
        if method.name == method_name:
            _validate_signature(method, class_name, method_name)
            return method

    available = [m.name for m in target_class.methods]
    raise SystemExit(
        f"Method '{method_name}' not found in '{class_name}'. "
        f"Available: {', '.join(available) or '(none)'}"
    )


def _validate_signature(method: jt.MethodDeclaration,
                         class_name: str, method_name: str):
    rt = method.return_type
    if rt is None:
        raise SystemExit(
            f"'{class_name}.{method_name}' returns void — "
            "must return boolean or Boolean."
        )
    rt_name = rt.name if hasattr(rt, "name") else str(rt)
    if rt_name not in ("boolean", "Boolean"):
        raise SystemExit(
            f"'{class_name}.{method_name}' returns '{rt_name}' — "
            "not a String validator (must return boolean or Boolean)."
        )
    params = method.parameters
    if len(params) != 1:
        raise SystemExit(
            f"'{class_name}.{method_name}' takes {len(params)} parameter(s) — "
            "must accept exactly one String parameter."
        )
    pt = params[0].type.name if hasattr(params[0].type, "name") else str(params[0].type)
    if pt != "String":
        raise SystemExit(
            f"'{class_name}.{method_name}' accepts '{pt}' — "
            "must accept a String parameter."
        )


# ── Source extraction (for LLM fallback) ─────────────────────────────────────

def extract_method_source(source: str, method_name: str) -> str:
    import re
    lines = source.splitlines()
    sig_re = re.compile(rf'\b{re.escape(method_name)}\s*\(')
    start = next((i for i, l in enumerate(lines) if sig_re.search(l)), None)
    if start is None:
        return f"// Could not extract {method_name}"
    depth, end = 0, start
    found = False
    for i in range(start, len(lines)):
        for ch in lines[i]:
            if ch == "{": depth += 1; found = True
            elif ch == "}": depth -= 1
        if found and depth == 0:
            end = i; break
    return "\n".join(lines[start:end + 1])


# ── Pretty output ─────────────────────────────────────────────────────────────

RESET  = "\033[0m"
BOLD   = "\033[1m"
GREEN  = "\033[32m"
YELLOW = "\033[33m"
CYAN   = "\033[36m"
RED    = "\033[31m"
DIM    = "\033[2m"


def print_header(class_name: str, method_name: str):
    print(f"\n{BOLD}{'─'*60}{RESET}")
    print(f"{BOLD}  Validator:{RESET} {CYAN}{class_name}.{method_name}{RESET}")
    print(f"{BOLD}{'─'*60}{RESET}\n")


def print_constraint_tree(constraint, indent: int = 0, verbose: bool = False):
    if not verbose:
        return
    pad = "  " * indent
    name = constraint.__class__.__name__
    detail = ""
    if hasattr(constraint, "pattern"):
        detail = f" pattern={constraint.pattern!r}"
    elif hasattr(constraint, "value") and not hasattr(constraint, "children"):
        detail = f" {getattr(constraint, 'op', '')} {constraint.value!r}"
    elif hasattr(constraint, "prefix"):
        detail = f" {constraint.prefix!r}"
    elif hasattr(constraint, "suffix"):
        detail = f" {constraint.suffix!r}"
    elif hasattr(constraint, "description"):
        detail = f" [{constraint.description}]"
    color = RED if name == "OpaqueConstraint" else DIM
    print(f"{pad}{color}{name}{detail}{RESET}")
    for attr in ("children", "child"):
        val = getattr(constraint, attr, None)
        if val is None:
            continue
        if isinstance(val, list):
            for child in val:
                print_constraint_tree(child, indent + 1, verbose)
        else:
            print_constraint_tree(val, indent + 1, verbose)


def print_results(strings: list[str], method: str, used_llm: bool, analysis: str = ""):
    tag = (f"{YELLOW}⚠  LLM used{RESET}" if used_llm
           else f"{GREEN}✓  Symbolic solver{RESET}")
    print(f"  {BOLD}Method:{RESET}   {method}")
    print(f"  {BOLD}Mode:{RESET}     {tag}")
    if analysis:
        print(f"  {BOLD}Analysis:{RESET} {analysis}")
    print()
    for i, s in enumerate(strings, 1):
        print(f"  {DIM}{i:>3}.{RESET}  {s!r}")
    print()


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Generate valid strings for a Java string validator."
    )
    parser.add_argument("java_file")
    parser.add_argument("method", help="ClassName.methodName")
    parser.add_argument("--count", "-n", type=int, default=10)
    parser.add_argument("--verbose", "-v", action="store_true",
                        help="Print extracted constraint tree")
    args = parser.parse_args()

    parts = args.method.rsplit(".", 1)
    if len(parts) != 2:
        raise SystemExit(f"Method must be 'ClassName.methodName', got '{args.method}'")
    class_name, method_name = parts

    print(f"{DIM}Loading {args.java_file}...{RESET}", file=sys.stderr)
    source = load_source(args.java_file)

    print(f"{DIM}Parsing {class_name}.{method_name}...{RESET}", file=sys.stderr)
    method_decl = parse_and_find_method(source, class_name, method_name)

    print_header(class_name, method_name)

    # ── Extract constraint tree ──
    extractor = ConstraintExtractor(method_decl)
    constraint = extractor.extract()

    if args.verbose:
        print(f"{BOLD}Constraint tree:{RESET}")
        print_constraint_tree(constraint, indent=1, verbose=True)
        print()

    # ── Attempt symbolic generation ──
    result = symbolic_generate(constraint, args.count)

    if not result.used_llm:
        print_results(result.strings, result.method, used_llm=False)
        return

    # ── LLM fallback ──
    print(f"{DIM}Symbolic solver insufficient ({result.method}). Invoking LLM...{RESET}",
          file=sys.stderr)
    method_source = extract_method_source(source, method_name)
    try:
        llm_result = generate_with_llm(
            method_source, class_name, method_name,
            args.count, partial_reason=result.method
        )
    except RuntimeError as e:
        raise SystemExit(f"LLM fallback failed: {e}")

    print_results(
        llm_result.get("examples", []),
        method=f"LLM (claude-opus-4-5) — {llm_result.get('analysis', '')}",
        used_llm=True,
        analysis=""
    )


if __name__ == "__main__":
    main()
