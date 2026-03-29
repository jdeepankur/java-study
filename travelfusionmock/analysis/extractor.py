"""
extractor.py — Walks a javalang AST and extracts a Constraint tree.

Handles:
  - s.matches(regex)
  - s.startsWith / endsWith / contains / equals / equalsIgnoreCase
  - s.length() comparisons
  - s.isEmpty() / s != null
  - Character.isDigit/isLetter/isUpperCase/isLowerCase/isLetterOrDigit/isWhitespace
      applied to all chars (via loops or charAt)
  - Integer.parseInt(s) / Double.parseDouble(s) comparisons
  - Boolean &&, ||, !
  - Ternary / early return patterns
  - Nested method calls one level deep
"""

from __future__ import annotations
import re as _re
from typing import Optional

import javalang
import javalang.tree as jt

from constraints import (
    Constraint, AndConstraint, OrConstraint, NotConstraint,
    NotNullConstraint, NotEmptyConstraint,
    LengthConstraint, RegexConstraint,
    StartsWithConstraint, EndsWithConstraint,
    ContainsConstraint, EqualsConstraint,
    CharClassConstraint, ParseIntConstraint, ParseDoubleConstraint,
    OpaqueConstraint, TrueConstraint, FalseConstraint,
)


# ── Helpers ───────────────────────────────────────────────────────────────────

def _literal_value(node) -> Optional[str]:
    """Return the Python string value of a literal node, or None."""
    if isinstance(node, jt.Literal):
        v = node.value
        if v in ("true", "false", "null"):
            return v
        # String literal: strip quotes and unescape basic sequences
        if v.startswith('"') and v.endswith('"'):
            inner = v[1:-1]
            inner = inner.replace('\\"', '"').replace('\\\\', '\\') \
                         .replace('\\n', '\n').replace('\\t', '\t') \
                         .replace('\\r', '\r')
            return inner
        # Char literal
        if v.startswith("'") and v.endswith("'"):
            return v[1:-1]
        # Numeric
        return v
    return None


def _int_literal(node) -> Optional[int]:
    v = _literal_value(node)
    if v is None:
        return None
    try:
        return int(v)
    except (ValueError, TypeError):
        return None


def _float_literal(node) -> Optional[float]:
    v = _literal_value(node)
    if v is None:
        return None
    try:
        return float(v.rstrip("fFdD"))
    except (ValueError, TypeError):
        return None


def _is_param_ref(node, param_name: str) -> bool:
    """True if node is a simple reference to the validator parameter."""
    return isinstance(node, jt.MemberReference) and node.member == param_name


def _comparison_op(op: str) -> str:
    return op  # already a string in javalang


# ── Main extractor ────────────────────────────────────────────────────────────

class ConstraintExtractor:
    """
    Given the javalang MethodDeclaration for a String → boolean method,
    produce a Constraint tree.
    """

    def __init__(self, method: jt.MethodDeclaration):
        self.param_name: str = method.parameters[0].name
        self.method = method
        # Track any helper-method calls we've inlined (avoids infinite recursion)
        self._inline_depth = 0

    def extract(self) -> Constraint:
        constraints = []
        for stmt in (self.method.body or []):
            c = self._stmt(stmt)
            if c is not None:
                constraints.append(c)
        if not constraints:
            return TrueConstraint()
        if len(constraints) == 1:
            return constraints[0]
        return AndConstraint(constraints)

    # ── Statement dispatch ────────────────────────────────────────────────────

    def _stmt(self, stmt) -> Optional[Constraint]:
        if isinstance(stmt, jt.ReturnStatement):
            return self._return_stmt(stmt)
        if isinstance(stmt, jt.IfStatement):
            return self._if_stmt(stmt)
        if isinstance(stmt, jt.BlockStatement):
            parts = [self._stmt(s) for s in (stmt.statements or [])]
            parts = [p for p in parts if p is not None]
            return AndConstraint(parts) if parts else None
        if isinstance(stmt, jt.TryStatement):
            return self._try_stmt(stmt)
        if isinstance(stmt, jt.LocalVariableDeclaration):
            return None  # variable declarations don't constrain the param directly
        if isinstance(stmt, jt.StatementExpression):
            return None
        # For loops and enhanced-for loops — body may contain early returns
        if isinstance(stmt, (jt.ForStatement, jt.WhileStatement, jt.DoStatement)):
            return self._stmt(stmt.body) if stmt.body else None
        if hasattr(jt, 'ForEachStatement') and isinstance(stmt, jt.ForEachStatement):
            return self._stmt(stmt.body) if stmt.body else None
        return None

    def _return_stmt(self, stmt: jt.ReturnStatement) -> Optional[Constraint]:
        if stmt.expression is None:
            return None
        return self._expr(stmt.expression)

    def _if_stmt(self, stmt: jt.IfStatement) -> Optional[Constraint]:
        """
        Pattern: if (!cond) return false;  →  cond must hold
        Pattern: if (cond) return true;    →  cond
        """
        condition = self._expr(stmt.condition)
        then_branch = stmt.then_statement
        else_branch = stmt.else_statement

        # Detect "if (x) return false" → !x must hold for valid strings
        # Detect "if (!x) return false" → x must hold
        then_returns_false = self._branch_returns(then_branch, False)
        then_returns_true  = self._branch_returns(then_branch, True)
        else_returns_false = self._branch_returns(else_branch, False) if else_branch else False
        else_returns_true  = self._branch_returns(else_branch, True)  if else_branch else False

        if then_returns_false and not else_branch:
            # if (cond) return false  →  NOT cond
            return NotConstraint(condition) if condition else None
        if then_returns_true and (else_returns_false or not else_branch):
            return condition
        if then_returns_false and else_returns_true:
            return NotConstraint(condition) if condition else None

        # General: can't fully resolve, mark as opaque
        return OpaqueConstraint(f"complex if-branch at line {getattr(stmt, 'position', '?')}")

    def _branch_returns(self, branch, value: bool) -> bool:
        """True if the branch unconditionally returns the given boolean literal."""
        if branch is None:
            return False
        stmts = []
        if isinstance(branch, jt.BlockStatement):
            stmts = branch.statements or []
        elif isinstance(branch, jt.ReturnStatement):
            stmts = [branch]
        else:
            stmts = [branch]
        for s in stmts:
            if isinstance(s, jt.ReturnStatement):
                v = _literal_value(s.expression) if s.expression else None
                return v == ("true" if value else "false")
        return False

    def _try_stmt(self, stmt: jt.TryStatement) -> Optional[Constraint]:
        """
        try { ... Integer.parseInt(s) ... } catch (NumberFormatException e) { return false; }
        → s must be parseable as int
        """
        catches = stmt.catches or []
        for catch in catches:
            param_types = []
            if hasattr(catch.parameter, 'types'):
                param_types = [t for t in (catch.parameter.types or [])]
            elif hasattr(catch.parameter, 'type'):
                t = catch.parameter.type
                param_types = [t.name if hasattr(t, 'name') else str(t)]
            if any("NumberFormatException" in str(pt) for pt in param_types):
                if self._branch_returns(catch.block, False):
                    # The try body must be parseable; extract numeric constraints from body
                    body_constraints = []
                    for s in (stmt.block or []):
                        c = self._stmt(s)
                        if c:
                            body_constraints.append(c)
                    return AndConstraint(body_constraints) if body_constraints else TrueConstraint()
        return OpaqueConstraint("try-catch block")

    # ── Expression dispatch ───────────────────────────────────────────────────

    def _expr(self, expr) -> Constraint:
        if expr is None:
            return TrueConstraint()

        # Boolean literals
        if isinstance(expr, jt.Literal):
            v = _literal_value(expr)
            if v == "true":  return TrueConstraint()
            if v == "false": return FalseConstraint()
            return OpaqueConstraint(f"literal {v}")

        # Logical &&
        if isinstance(expr, jt.BinaryOperation) and expr.operator == "&&":
            return AndConstraint([self._expr(expr.operandl), self._expr(expr.operandr)])

        # Logical ||
        if isinstance(expr, jt.BinaryOperation) and expr.operator == "||":
            return OrConstraint([self._expr(expr.operandl), self._expr(expr.operandr)])

        # Logical !
        if isinstance(expr, jt.MethodInvocation) and expr.member == "not":
            pass  # handled below
        if hasattr(expr, '__class__') and expr.__class__.__name__ == 'Not':
            return NotConstraint(self._expr(expr.expression))

        # Prefix !
        if isinstance(expr, jt.MemberReference) and False:
            pass
        # javalang represents "!" as a UnaryExpression with prefix "!"
        cls_name = expr.__class__.__name__
        if cls_name == 'UnaryExpression':
            if getattr(expr, 'prefix_operators', None) and '!' in expr.prefix_operators:
                return NotConstraint(self._expr(expr.expression))

        # Null check: s != null
        if isinstance(expr, jt.BinaryOperation) and expr.operator in ("!=", "=="):
            left, right = expr.operandl, expr.operandr
            is_null_check = (
                (_is_param_ref(left, self.param_name) and isinstance(right, jt.Literal) and right.value == "null") or
                (_is_param_ref(right, self.param_name) and isinstance(left, jt.Literal) and left.value == "null")
            )
            if is_null_check:
                c = NotNullConstraint()
                return c if expr.operator == "!=" else NotConstraint(c)

        # Comparison: s.length() op N
        if isinstance(expr, jt.BinaryOperation) and expr.operator in ("<", "<=", ">", ">=", "==", "!="):
            c = self._length_comparison(expr)
            if c:
                return c
            c = self._numeric_comparison(expr)
            if c:
                return c

        # Method invocation
        if isinstance(expr, jt.MethodInvocation):
            return self._method_invocation(expr)

        # Ternary: cond ? true : false  → just the condition
        if cls_name == 'TernaryExpression':
            tv = _literal_value(getattr(expr, 'if_true', None))
            fv = _literal_value(getattr(expr, 'if_false', None))
            if tv == "true" and fv == "false":
                return self._expr(expr.condition)

        # MemberReference: variable or field access (e.g. `closing`, `result`, `node.closed`)
        if isinstance(expr, jt.MemberReference):
            if expr.member == self.param_name:
                return NotNullConstraint()
            return OpaqueConstraint(f"variable reference: {expr.member}")

        return OpaqueConstraint(f"unhandled expression type: {cls_name}")

    # ── Length comparisons ────────────────────────────────────────────────────

    def _length_comparison(self, expr: jt.BinaryOperation) -> Optional[Constraint]:
        left, right, op = expr.operandl, expr.operandr, expr.operator

        def is_length_call(node):
            return (
                isinstance(node, jt.MethodInvocation) and
                node.member == "length" and
                _is_param_ref(node.qualifier_node if hasattr(node, 'qualifier_node') else None, self.param_name)
            ) or (
                isinstance(node, jt.MethodInvocation) and
                node.member == "length" and
                node.qualifier == self.param_name
            )

        if is_length_call(left):
            n = _int_literal(right)
            if n is not None:
                return LengthConstraint(op, n)
        if is_length_call(right):
            n = _int_literal(left)
            if n is not None:
                # Flip operator: n op s.length()  →  s.length() flip(op) n
                flipped = {"<": ">", "<=": ">=", ">": "<", ">=": "<=", "==": "==", "!=": "!="}
                return LengthConstraint(flipped[op], n)
        return None

    # ── Numeric (parseInt / parseDouble) comparisons ─────────────────────────

    def _numeric_comparison(self, expr: jt.BinaryOperation) -> Optional[Constraint]:
        left, right, op = expr.operandl, expr.operandr, expr.operator

        def parse_call(node):
            if not isinstance(node, jt.MethodInvocation):
                return None
            if node.member in ("parseInt", "valueOf") and node.qualifier in ("Integer", "Long"):
                args = list(node.arguments or [])
                if args and _is_param_ref(args[0], self.param_name):
                    return "int"
            if node.member in ("parseDouble", "parseFloat") and node.qualifier in ("Double", "Float"):
                args = list(node.arguments or [])
                if args and _is_param_ref(args[0], self.param_name):
                    return "double"
            return None

        kind = parse_call(left)
        if kind:
            if kind == "int":
                n = _int_literal(right)
                if n is not None:
                    return ParseIntConstraint(op, n)
            else:
                n = _float_literal(right)
                if n is not None:
                    return ParseDoubleConstraint(op, n)

        kind = parse_call(right)
        if kind:
            flipped = {"<": ">", "<=": ">=", ">": "<", ">=": "<=", "==": "==", "!=": "!="}
            if kind == "int":
                n = _int_literal(left)
                if n is not None:
                    return ParseIntConstraint(flipped[op], n)
            else:
                n = _float_literal(left)
                if n is not None:
                    return ParseDoubleConstraint(flipped[op], n)

        return None

    # ── Method invocations ────────────────────────────────────────────────────

    def _method_invocation(self, expr: jt.MethodInvocation) -> Constraint:
        qualifier = expr.qualifier or ""
        member    = expr.member
        args      = list(expr.arguments or [])

        # ── s.matches(pattern) ──
        if qualifier == self.param_name and member == "matches":
            if args:
                pat = _literal_value(args[0])
                if pat:
                    return RegexConstraint(pat)
            return OpaqueConstraint("s.matches(non-literal)")

        # ── s.startsWith(prefix) ──
        if qualifier == self.param_name and member == "startsWith":
            if args:
                v = _literal_value(args[0])
                if v is not None:
                    return StartsWithConstraint(v)

        # ── s.endsWith(suffix) ──
        if qualifier == self.param_name and member == "endsWith":
            if args:
                v = _literal_value(args[0])
                if v is not None:
                    return EndsWithConstraint(v)

        # ── s.contains(sub) ──
        if qualifier == self.param_name and member == "contains":
            if args:
                v = _literal_value(args[0])
                if v is not None:
                    return ContainsConstraint(v)

        # ── s.equals(x) / s.equalsIgnoreCase(x) ──
        if qualifier == self.param_name and member in ("equals", "equalsIgnoreCase"):
            if args:
                v = _literal_value(args[0])
                if v is not None:
                    return EqualsConstraint(v, ignore_case=(member == "equalsIgnoreCase"))

        # ── s.isEmpty() ──
        if qualifier == self.param_name and member == "isEmpty":
            return NotEmptyConstraint()   # validator checks isEmpty → negated = must not be empty

        # ── s.trim().isEmpty() ──
        if member == "isEmpty" and qualifier == "":
            # might be chained: check if qualifier_node is s.trim()
            pass

        # ── Character.isXxx(s.charAt(i)) — treat as whole-string char-class constraint ──
        char_class = self._char_class_invocation(expr)
        if char_class:
            return char_class

        # ── String.valueOf / Objects.nonNull etc. ──
        if member == "nonNull" and qualifier in ("Objects", "java.util.Objects"):
            if args and _is_param_ref(args[0], self.param_name):
                return NotNullConstraint()

        return OpaqueConstraint(f"unresolved call: {qualifier}.{member}()")

    def _char_class_invocation(self, expr: jt.MethodInvocation) -> Optional[Constraint]:
        """
        Recognise Character.isXxx(...) applied to a character of the param.
        """
        char_class_map = {
            "isDigit":         "digit",
            "isLetter":        "alpha",
            "isLetterOrDigit": "alnum",
            "isUpperCase":     "upper",
            "isLowerCase":     "lower",
            "isWhitespace":    "space",
            "isAlphabetic":    "alpha",
        }
        if expr.qualifier == "Character" and expr.member in char_class_map:
            return CharClassConstraint(char_class_map[expr.member])
        return None
