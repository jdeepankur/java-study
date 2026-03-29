"""
solver.py — Symbolic string generator.

Walks a Constraint tree and produces N strings satisfying it,
without invoking any LLM.

Strategy per constraint type:
  - RegexConstraint       → rstr / exrex
  - LengthConstraint      → clamp a candidate's length
  - StartsWithConstraint  → prepend prefix, fill remainder
  - EndsWithConstraint    → append suffix, fill prefix
  - ContainsConstraint    → embed substring, fill around
  - EqualsConstraint      → return exact string
  - CharClassConstraint   → draw characters from class alphabet
  - ParseIntConstraint    → enumerate integers satisfying condition
  - ParseDoubleConstraint → enumerate doubles satisfying condition
  - NotNullConstraint     → satisfied by any non-None string
  - NotEmptyConstraint    → satisfied by any non-empty string
  - AndConstraint         → intersect constraints (length bounds + alphabet + regex)
  - OrConstraint          → union: draw from each branch
  - NotConstraint         → limited support (negate simple constraints)
  - TrueConstraint        → any string
  - FalseConstraint       → no string (impossible)
  - OpaqueConstraint      → signal that LLM is needed
"""

from __future__ import annotations
import random
import string
from typing import Optional

from constraints import (
    Constraint,
    AndConstraint,
    OrConstraint,
    NotConstraint,
    NotNullConstraint,
    NotEmptyConstraint,
    LengthConstraint,
    RegexConstraint,
    StartsWithConstraint,
    EndsWithConstraint,
    ContainsConstraint,
    EqualsConstraint,
    CharClassConstraint,
    ParseIntConstraint,
    ParseDoubleConstraint,
    OpaqueConstraint,
    TrueConstraint,
    FalseConstraint,
)

# ── Character pools ───────────────────────────────────────────────────────────

POOLS = {
    "digit": string.digits,
    "alpha": string.ascii_letters,
    "alnum": string.ascii_letters + string.digits,
    "upper": string.ascii_uppercase,
    "lower": string.ascii_lowercase,
    "space": " \t\n",
    "ascii_printable": string.printable.strip(),
    "any": string.ascii_letters + string.digits + " !@#$%^&*()-_=+[]{}|;:',.<>?/`~",
}


# ── Solved context (accumulated constraints for one generation pass) ───────────


class SolvedContext:
    """
    Accumulates length bounds, required prefix/suffix/contains,
    character pool, and a list of regex patterns,
    all to be intersected during generation.
    """

    def __init__(self):
        self.min_length: int = 0
        self.max_length: int = 50
        self.exact_length: Optional[int] = None
        self.prefix: str = ""
        self.suffix: str = ""
        self.contains: list[str] = []
        self.char_pool: Optional[str] = None  # None = any
        self.regex_patterns: list[str] = []
        self.fixed_value: Optional[str] = None  # EqualsConstraint
        self.impossible: bool = False
        self.opaque: bool = False
        self.opaque_reason: str = ""
        # Numeric constraints produce strings directly
        self.int_constraints: list[tuple[str, int]] = []  # (op, value)
        self.double_constraints: list[tuple[str, float]] = []

    def intersect_pool(self, pool: str):
        if self.char_pool is None:
            self.char_pool = pool
        else:
            merged = "".join(c for c in self.char_pool if c in pool)
            self.char_pool = merged if merged else ""

    def effective_pool(self) -> str:
        return self.char_pool if self.char_pool is not None else POOLS["any"]

    def effective_length_range(self) -> tuple[int, int]:
        lo = self.min_length
        hi = self.max_length
        if self.exact_length is not None:
            lo = hi = self.exact_length
        # Prefix/suffix/contains impose a floor
        floor = len(self.prefix) + len(self.suffix) + sum(len(c) for c in self.contains)
        lo = max(lo, floor)
        if lo > hi:
            self.impossible = True
        return lo, hi


def _apply_length(ctx: SolvedContext, c: LengthConstraint):
    op, v = c.op, c.value
    if op == "==":
        ctx.exact_length = v
    elif op == "!=":
        pass  # hard to enforce simply; skip (generates some invalid, filtered later)
    elif op == ">":
        ctx.min_length = max(ctx.min_length, v + 1)
    elif op == ">=":
        ctx.min_length = max(ctx.min_length, v)
    elif op == "<":
        ctx.max_length = min(ctx.max_length, v - 1)
    elif op == "<=":
        ctx.max_length = min(ctx.max_length, v)


def _build_context(constraint: Constraint, ctx: SolvedContext):
    """Recursively populate ctx from a constraint tree."""

    if ctx.impossible or ctx.opaque:
        return

    if isinstance(constraint, TrueConstraint):
        return

    if isinstance(constraint, FalseConstraint):
        ctx.impossible = True
        return

    if isinstance(constraint, OpaqueConstraint):
        ctx.opaque = True
        ctx.opaque_reason = constraint.description
        return

    if isinstance(constraint, AndConstraint):
        for child in constraint.children:
            _build_context(child, ctx)
        return

    if isinstance(constraint, OrConstraint):
        # For OrConstraint we pick the first non-impossible branch at generation time.
        # Here, just mark as needing special handling by storing the Or.
        # We handle it in generate() directly.
        ctx.opaque = True
        ctx.opaque_reason = "__or__"  # special sentinel
        ctx._or_constraint = constraint  # type: ignore[attr-defined]
        return

    if isinstance(constraint, NotConstraint):
        # Only handle simple negations
        inner = constraint.child
        if isinstance(inner, NotNullConstraint):
            ctx.impossible = True  # null string → impossible in practice
        elif isinstance(inner, NotEmptyConstraint):
            ctx.exact_length = 0
        elif isinstance(inner, LengthConstraint):
            # negate length constraint
            op_neg = {
                "==": "!=",
                "!=": "==",
                "<": ">=",
                "<=": ">",
                ">": "<=",
                ">=": "<",
            }
            _apply_length(ctx, LengthConstraint(op_neg[inner.op], inner.value))
        elif isinstance(inner, FalseConstraint):
            return  # NOT false → true, no constraint
        else:
            ctx.opaque = True
            ctx.opaque_reason = f"negation of {inner.__class__.__name__}"
        return

    if isinstance(constraint, NotNullConstraint):
        return  # satisfied by any Python string

    if isinstance(constraint, NotEmptyConstraint):
        ctx.min_length = max(ctx.min_length, 1)
        return

    if isinstance(constraint, LengthConstraint):
        _apply_length(ctx, constraint)
        return

    if isinstance(constraint, RegexConstraint):
        pat = constraint.pattern
        if constraint.negated:
            ctx.opaque = True
            ctx.opaque_reason = "negated regex"
        else:
            ctx.regex_patterns.append(pat)
        return

    if isinstance(constraint, StartsWithConstraint):
        if constraint.negated:
            pass  # ignore negated prefix for now
        else:
            ctx.prefix = ctx.prefix + constraint.prefix  # accumulate
        return

    if isinstance(constraint, EndsWithConstraint):
        if constraint.negated:
            pass
        else:
            ctx.suffix = constraint.suffix + ctx.suffix
        return

    if isinstance(constraint, ContainsConstraint):
        if not constraint.negated:
            ctx.contains.append(constraint.substring)
        return

    if isinstance(constraint, EqualsConstraint):
        if constraint.negated:
            ctx.opaque = True
            ctx.opaque_reason = "negated equals"
        else:
            ctx.fixed_value = constraint.value
        return

    if isinstance(constraint, CharClassConstraint):
        if constraint.negated:
            ctx.opaque = True
            ctx.opaque_reason = "negated char class"
        else:
            pool = POOLS.get(constraint.char_class, POOLS["any"])
            ctx.intersect_pool(pool)
        return

    if isinstance(constraint, ParseIntConstraint):
        ctx.int_constraints.append((constraint.op, constraint.value))
        return

    if isinstance(constraint, ParseDoubleConstraint):
        ctx.double_constraints.append((constraint.op, constraint.value))
        return

    ctx.opaque = True
    ctx.opaque_reason = f"unhandled: {constraint.__class__.__name__}"


# ── Regex generation ──────────────────────────────────────────────────────────


def _try_import_regex_gen():
    try:
        import exrex

        return exrex
    except ImportError:
        pass
    try:
        import rstr

        return rstr
    except ImportError:
        pass
    return None


def _generate_from_regex(pattern: str, n: int) -> Optional[list[str]]:
    lib = _try_import_regex_gen()
    if lib is None:
        return None
    results: set[str] = set()
    attempts = 0
    max_attempts = n * 200
    while len(results) < n and attempts < max_attempts:
        attempts += 1
        try:
            if hasattr(lib, "getone"):
                s: str = lib.getone(pattern)
            elif hasattr(lib, "xeger"):
                s: str = lib.xeger(pattern)
            else:
                s: str = next(lib.generate(pattern, limit=1)) 
            results.add(s)
        except Exception:
            break
    return list(results) if results else None


# ── Integer / double generators ───────────────────────────────────────────────


def _satisfies_int_constraints(v: int, constraints: list[tuple[str, int]]) -> bool:
    for op, n in constraints:
        if op == "==" and not (v == n):
            return False
        if op == "!=" and not (v != n):
            return False
        if op == "<" and not (v < n):
            return False
        if op == "<=" and not (v <= n):
            return False
        if op == ">" and not (v > n):
            return False
        if op == ">=" and not (v >= n):
            return False
    return True


def _generate_int_strings(constraints: list[tuple[str, int]], n: int) -> list[str]:
    """Enumerate integers that satisfy all constraints."""
    # Compute a search range from the constraints
    lo, hi = -(10**9), 10**9
    for op, v in constraints:
        if op == "==":
            lo = hi = v
            break
        if op == ">":
            lo = max(lo, v + 1)
        if op == ">=":
            lo = max(lo, v)
        if op == "<":
            hi = min(hi, v - 1)
        if op == "<=":
            hi = min(hi, v)

    results = []
    # Try a spread of values in [lo, hi]
    candidates = list(range(lo, min(lo + 1000, hi + 1)))
    random.shuffle(candidates)
    for v in candidates:
        if _satisfies_int_constraints(v, constraints):
            results.append(str(v))
            if len(results) == n:
                break
    return results


# ── Core generator ────────────────────────────────────────────────────────────


def _fill_string(
    ctx: SolvedContext, existing_prefix: str = "", existing_suffix: str = ""
) -> str:
    """Generate one string that satisfies length + pool constraints."""
    lo, hi = ctx.effective_length_range()
    lo = max(lo, 0)
    hi = max(hi, lo)
    pool = ctx.effective_pool()
    if not pool:
        pool = POOLS["any"]

    target_len = random.randint(lo, min(hi, lo + 20))
    body_len = target_len - len(ctx.prefix) - len(ctx.suffix)

    # Insert contains substrings in the body
    body = ""
    for sub in ctx.contains:
        body += sub
    body_len = max(body_len - len(body), 0)
    body += "".join(random.choice(pool) for _ in range(body_len))

    return ctx.prefix + body + ctx.suffix


class SolverResult:
    def __init__(self, strings: list[str], used_llm: bool, method: str):
        self.strings = strings
        self.used_llm = used_llm
        self.method = method  # human-readable description of what was used


def generate(constraint: Constraint, n: int) -> SolverResult:
    """
    Main entry point. Returns a SolverResult indicating whether
    symbolic solving succeeded or the LLM is required.
    """

    # ── Handle OrConstraint by solving each branch (always, before ctx build) ─
    if isinstance(constraint, OrConstraint):
        return _generate_or(constraint, n)

    # ── Handle AndConstraint that contains an OrConstraint ─────────────────
    if isinstance(constraint, AndConstraint):
        or_children = [c for c in constraint.children if isinstance(c, OrConstraint)]
        if or_children:
            # Try each branch of the first Or, filtered by the rest of the And
            other = [c for c in constraint.children if not isinstance(c, OrConstraint)]
            and_ctx = SolvedContext()
            for c in other:
                _build_context(c, and_ctx)
            if and_ctx.opaque and and_ctx.opaque_reason != "__or__":
                return SolverResult(
                    [], used_llm=True, method="LLM (opaque sub-constraint in And)"
                )
            results = []
            for branch in or_children[0].children:
                sub = AndConstraint([branch] + other) if other else branch
                r = generate(sub, n - len(results))
                if r.used_llm:
                    return SolverResult([], used_llm=True, method=r.method)
                results.extend(r.strings)
                if len(results) >= n:
                    break
            return SolverResult(
                results[:n], used_llm=False, method="symbolic (Or branch expansion)"
            )

    ctx = SolvedContext()
    _build_context(constraint, ctx)

    # ── Opaque → need LLM ──────────────────────────────────────────────────
    if ctx.opaque and ctx.opaque_reason != "__or__":
        return SolverResult(
            [], used_llm=True, method=f"LLM (opaque: {ctx.opaque_reason})"
        )

    if ctx.impossible:
        return SolverResult(
            [],
            used_llm=False,
            method="symbolic (impossible constraint — no valid strings)",
        )

    # ── Fixed value ────────────────────────────────────────────────────────
    if ctx.fixed_value is not None:
        return SolverResult(
            [ctx.fixed_value] * n, used_llm=False, method="symbolic (exact value)"
        )

    # ── Integer strings ────────────────────────────────────────────────────
    if ctx.int_constraints:
        strings = _generate_int_strings(ctx.int_constraints, n)
        if len(strings) >= n:
            return SolverResult(
                strings[:n], used_llm=False, method="symbolic (integer enumeration)"
            )
        # Not enough; fall through to regex/fill with what we have
        if strings:
            return SolverResult(
                strings, used_llm=False, method="symbolic (partial integer enumeration)"
            )

    # ── Regex patterns ─────────────────────────────────────────────────────
    if ctx.regex_patterns:
        # Combine multiple patterns with lookaheads if >1
        if len(ctx.regex_patterns) == 1:
            combined = ctx.regex_patterns[0]
        else:
            # Use lookahead intersection: (?=p1)(?=p2)....*
            lookaheads = "".join(f"(?={p})" for p in ctx.regex_patterns[:-1])
            combined = lookaheads + ctx.regex_patterns[-1]

        strings = _generate_from_regex(combined, n)
        if strings and len(strings) >= n:
            return SolverResult(
                strings[:n],
                used_llm=False,
                method=f"symbolic (regex generation: {combined!r})",
            )
        if strings:
            # Partial — pad with fill if possible
            while len(strings) < n:
                s = _fill_string(ctx)
                if _matches_all_regex(s, ctx.regex_patterns):
                    strings.append(s)
            return SolverResult(
                strings[:n],
                used_llm=False,
                method=f"symbolic (regex + fill: {combined!r})",
            )

        # exrex/rstr not available — use alternation-aware structured generator
        import re

        strings = _generate_from_pattern_structured(combined, n)
        if strings:
            return SolverResult(
                strings[:n],
                used_llm=False,
                method=f"symbolic (structured regex: {combined!r})",
            )

        # Still nothing — need LLM
        return SolverResult(
            [],
            used_llm=True,
            method=f"LLM (regex unsolvable symbolically: {combined!r})",
        )

    # ── Pure structural (prefix/suffix/length/char class) ──────────────────
    lo, hi = ctx.effective_length_range()
    if ctx.impossible:
        return SolverResult(
            [],
            used_llm=False,
            method="symbolic (impossible: length constraints unsatisfiable)",
        )

    strings = set()
    attempts = 0
    while len(strings) < n and attempts < n * 500:
        attempts += 1
        s = _fill_string(ctx)
        strings.add(s)

    method = "symbolic (structural: "
    parts = []
    if ctx.prefix:
        parts.append(f"prefix={ctx.prefix!r}")
    if ctx.suffix:
        parts.append(f"suffix={ctx.suffix!r}")
    if ctx.contains:
        parts.append(f"contains={ctx.contains}")
    if ctx.char_pool:
        parts.append(f"pool={ctx.char_pool[:20]!r}...")
    if ctx.exact_length:
        parts.append(f"len={ctx.exact_length}")
    elif lo or hi < 50:
        parts.append(f"len∈[{lo},{hi}]")
    method += (", ".join(parts) or "any") + ")"

    return SolverResult(list(strings)[:n], used_llm=False, method=method)


def _generate_or(constraint: OrConstraint, n: int) -> SolverResult:
    """Round-robin across Or branches, solving each independently."""
    results = []
    branches = constraint.children
    per_branch = max(1, (n + len(branches) - 1) // len(branches))
    any_llm = False
    methods = []
    for branch in branches:
        r = generate(branch, per_branch)
        results.extend(r.strings)
        if r.used_llm:
            any_llm = True
        methods.append(r.method)
        if len(results) >= n:
            break
    combined_method = "symbolic (Or: " + " | ".join(methods) + ")"
    return SolverResult(results[:n], used_llm=any_llm, method=combined_method)


def _length_range_from_pattern(pattern: str) -> tuple[int, int]:
    """
    Heuristically estimate the min/max string length implied by a regex pattern.
    This is a rough lower/upper bound, not a formal analysis.
    """
    import re

    # Remove anchors
    p = pattern.lstrip("^").rstrip("$")

    # Count fixed-length tokens and quantifiers
    # Each char class [..], \d, \w, \s, or literal char contributes some length
    # We'll do a rough token walk
    lo = hi = 0
    i = 0
    while i < len(p):
        # Escaped char
        if p[i] == "\\" and i + 1 < len(p):
            token_lo = token_hi = 1
            i += 2
        # Character class
        elif p[i] == "[":
            end = p.index("]", i + 1) if "]" in p[i + 1 :] else len(p)
            token_lo = token_hi = 1
            i = end + 1
        # Group — treat as opaque, assume 1..10
        elif p[i] == "(":
            depth = 1
            j = i + 1
            while j < len(p) and depth:
                if p[j] == "(":
                    depth += 1
                elif p[j] == ")":
                    depth -= 1
                j += 1
            token_lo, token_hi = 1, 10
            i = j
        # Dot
        elif p[i] == ".":
            token_lo = token_hi = 1
            i += 1
        # Literal
        elif p[i] not in "+*?{|)":
            token_lo = token_hi = 1
            i += 1
        else:
            i += 1
            continue

        # Check for quantifier following the token
        if i < len(p):
            if p[i] == "?":
                token_lo = 0
                i += 1
            elif p[i] == "*":
                token_lo = 0
                token_hi = 20
                i += 1
            elif p[i] == "+":
                token_hi = max(token_hi, 20)
                i += 1
            elif p[i] == "{":
                m = re.match(r"\{(\d+)(?:,(\d*))?\}", p[i:])
                if m:
                    token_lo = int(m.group(1))
                    if m.group(2) is None:
                        token_hi = token_lo
                    elif m.group(2) == "":
                        token_hi = token_lo * 3
                    else:
                        token_hi = int(m.group(2))
                    i += len(m.group(0))

        lo += token_lo
        hi += token_hi

    return max(1, lo), max(lo, hi, 1)


def _generate_from_pattern_structured(pattern: str, n: int) -> list[str]:
    """
    Generate strings by walking the regex token by token and randomly
    satisfying each token. Handles:
      - Literal characters
      - Character classes [...]
      - Shorthand classes \\d \\w \\s \\D \\W \\S
      - Dot (.)
      - Quantifiers ?, *, +, {m}, {m,n}
      - Non-capturing groups (?:...) with | alternations
      - Capturing groups (...) with | alternations
      - Anchors ^ $ (ignored)
    """
    import re

    # Strip anchors
    p = pattern
    if p.startswith("^"):
        p = p[1:]
    if p.endswith("$"):
        p = p[:-1]

    results = set()
    for _ in range(n * 100):
        try:
            s = _gen_pattern(p)
            if re.fullmatch(pattern, s):
                results.add(s)
                if len(results) >= n:
                    break
        except Exception:
            continue
    return list(results)


def _gen_pattern(pattern: str) -> str:
    """Recursively generate one string matching the pattern."""
    result = []
    i = 0
    while i < len(pattern):
        token, length, pool_or_sub = _next_token(pattern, i)
        i += length

        # Quantifier
        quant, qlen = _read_quantifier(pattern, i)
        i += qlen

        # Determine repetition count
        lo, hi = _quant_range(quant)
        count = random.randint(lo, min(hi, lo + 5))

        if token == "group":
            # pool_or_sub is the group content (string); may contain |
            for _ in range(count):
                branch = _pick_alternation_branch(pool_or_sub)
                result.append(_gen_pattern(branch))
        elif token == "class":
            for _ in range(count):
                result.append(random.choice(pool_or_sub))
        elif token == "literal":
            result.append(pool_or_sub * count)
        elif token == "any":
            for _ in range(count):
                result.append(random.choice(string.ascii_letters + string.digits))

    return "".join(result)


def _next_token(pattern: str, i: int):
    """
    Returns (token_type, length_consumed, payload).
    token_type: 'literal' | 'class' | 'group' | 'any'
    payload: for 'literal' = the char, 'class' = pool string,
             'group' = inner pattern string, 'any' = None
    """
    if i >= len(pattern):
        return ("literal", 0, "")

    c = pattern[i]

    # Anchor chars — skip
    if c in "^$":
        return ("literal", 1, "")

    # Escaped sequence
    if c == "\\" and i + 1 < len(pattern):
        nc = pattern[i + 1]
        pool = _shorthand_pool(nc)
        if pool:
            return ("class", 2, pool)
        return ("literal", 2, nc)

    # Character class [...]
    if c == "[":
        end = _find_class_end(pattern, i)
        pool = _expand_char_class(pattern[i + 1 : end])
        return ("class", end - i + 1, pool)

    # Group (?: ...) or (...)
    if c == "(":
        end = _find_group_end(pattern, i)
        inner = pattern[i + 1 : end]
        # Strip ?: prefix for non-capturing groups
        if inner.startswith("?:"):
            inner = inner[2:]
        elif inner.startswith("?=") or inner.startswith("?!"):
            inner = ""  # lookahead — ignore
        return ("group", end - i + 1, inner)

    # Dot
    if c == ".":
        return ("any", 1, None)

    # Pipe at top level — should be handled by caller via _pick_alternation_branch
    if c == "|":
        return ("literal", 1, "")

    return ("literal", 1, c)


def _read_quantifier(pattern: str, i: int):
    """Returns (quantifier_string, length_consumed)."""
    if i >= len(pattern):
        return ("1", 0)
    c = pattern[i]
    if c == "?":
        return ("?", 1)
    if c == "*":
        return ("*", 1)
    if c == "+":
        return ("+", 1)
    if c == "{":
        import re

        m = re.match(r"\{(\d+)(?:,(\d*))?\}", pattern[i:])
        if m:
            return (m.group(0), len(m.group(0)))
    return ("1", 0)


def _quant_range(quant: str) -> tuple[int, int]:
    if quant == "?":
        return (0, 1)
    if quant == "*":
        return (0, 4)
    if quant == "+":
        return (1, 5)
    if quant == "1":
        return (1, 1)
    import re

    m = re.match(r"\{(\d+)(?:,(\d*))?\}", quant)
    if m:
        lo = int(m.group(1))
        if m.group(2) is None:
            return (lo, lo)
        if m.group(2) == "":
            return (lo, lo + 3)
        return (lo, int(m.group(2)))
    return (1, 1)


def _find_class_end(pattern: str, start: int) -> int:
    """Find the closing ] for a character class starting at start."""
    i = start + 1
    if i < len(pattern) and pattern[i] == "^":
        i += 1
    if i < len(pattern) and pattern[i] == "]":
        i += 1  # literal ] at start
    while i < len(pattern):
        if pattern[i] == "\\":
            i += 2
            continue
        if pattern[i] == "]":
            return i
        i += 1
    return len(pattern) - 1


def _find_group_end(pattern: str, start: int) -> int:
    """Find the closing ) for a group starting at start."""
    depth = 0
    i = start
    while i < len(pattern):
        if pattern[i] == "\\":
            i += 2
            continue
        if pattern[i] == "(":
            depth += 1
        elif pattern[i] == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return len(pattern) - 1


def _expand_char_class(content: str) -> str:
    """Expand a character class body (without [ ]) into a pool string."""
    pool = []
    negated = content.startswith("^")
    if negated:
        content = content[1:]
    i = 0
    while i < len(content):
        if content[i] == "\\" and i + 1 < len(content):
            p = _shorthand_pool(content[i + 1])
            if p:
                pool.extend(p)
            else:
                pool.append(content[i + 1])
            i += 2
        elif i + 2 < len(content) and content[i + 1] == "-":
            for c in range(ord(content[i]), ord(content[i + 2]) + 1):
                pool.append(chr(c))
            i += 3
        else:
            pool.append(content[i])
            i += 1
    if negated:
        full = string.printable[:-5]
        pool = [c for c in full if c not in pool]
    return "".join(dict.fromkeys(pool))  # deduplicate preserving order


def _shorthand_pool(c: str) -> str:
    return {
        "d": string.digits,
        "D": string.ascii_letters + " ",
        "w": string.ascii_letters + string.digits + "_",
        "W": " !@#$%^&*()",
        "s": " \t\n",
        "S": string.ascii_letters + string.digits,
    }.get(c, "")


def _pick_alternation_branch(pattern: str) -> str:
    """
    Split a pattern on top-level | and pick one branch randomly.
    Respects nested groups.
    """
    branches = []
    depth = 0
    current = []
    i = 0
    while i < len(pattern):
        c = pattern[i]
        if c == "\\":
            current.append(pattern[i : i + 2])
            i += 2
            continue
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
        if c == "|" and depth == 0:
            branches.append("".join(current))
            current = []
        else:
            current.append(c)
        i += 1
    branches.append("".join(current))
    return random.choice(branches)


def _matches_all_regex(s: str, patterns: list[str]) -> bool:
    import re

    for p in patterns:
        # Java .matches() is fully anchored
        if not re.fullmatch(p, s):
            return False
    return True


def _pool_from_pattern(pattern: str) -> str:
    """
    Heuristically derive a useful character pool from a regex pattern.
    Looks for character class hints in the pattern and builds a pool
    biased toward characters likely to satisfy it.
    """
    import re

    pool_chars = set()

    # Explicit char classes [...]
    for m in re.finditer(r"\[([^\]]+)\]", pattern):
        content = m.group(1)
        # Expand ranges like a-z, 0-9, A-Z
        i = 0
        while i < len(content):
            if i + 2 < len(content) and content[i + 1] == "-":
                for c in range(ord(content[i]), ord(content[i + 2]) + 1):
                    pool_chars.add(chr(c))
                i += 3
            else:
                pool_chars.add(content[i])
                i += 1

    # Shorthand classes
    if r"\d" in pattern:
        pool_chars.update(string.digits)
    if r"\w" in pattern:
        pool_chars.update(string.ascii_letters + string.digits + "_")
    if r"\s" in pattern:
        pool_chars.update(" \t\n")
    if r"\D" not in pattern and r"\W" not in pattern:
        # No exclusions — add letters as default filler
        pool_chars.update(string.ascii_letters)

    # Literal characters outside of groups (rough heuristic)
    stripped = re.sub(r"\[.*?\]", "", pattern)
    stripped = re.sub(r"\\[dDwWsS.]", "", stripped)
    stripped = re.sub(r"[\\^$.*+?{}()|]", "", stripped)
    pool_chars.update(c for c in stripped if c.isprintable())

    result = "".join(sorted(pool_chars))
    return result if result else POOLS["any"]
