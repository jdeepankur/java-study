"""
constraints.py — Intermediate representation for string validator constraints.

A validator is reduced to a tree of Constraint nodes. The symbolic solver
operates on this tree directly. If any node is an OpaqueConstraint, the
solver cannot fully resolve it and falls back to the LLM.
"""

from __future__ import annotations
from dataclasses import dataclass, field
from typing import Optional


# ── Base ──────────────────────────────────────────────────────────────────────

class Constraint:
    """Base class for all constraints."""
    def is_opaque(self) -> bool:
        """Returns True if this subtree contains any opaque (unresolved) nodes."""
        return False


# ── Logical combinators ───────────────────────────────────────────────────────

@dataclass
class AndConstraint(Constraint):
    children: list[Constraint]

    def is_opaque(self) -> bool:
        return any(c.is_opaque() for c in self.children)


@dataclass
class OrConstraint(Constraint):
    children: list[Constraint]

    def is_opaque(self) -> bool:
        return any(c.is_opaque() for c in self.children)


@dataclass
class NotConstraint(Constraint):
    child: Constraint

    def is_opaque(self) -> bool:
        return self.child.is_opaque()


# ── Null / emptiness ──────────────────────────────────────────────────────────

@dataclass
class NotNullConstraint(Constraint):
    pass


@dataclass
class NotEmptyConstraint(Constraint):
    pass


# ── Length ────────────────────────────────────────────────────────────────────

@dataclass
class LengthConstraint(Constraint):
    """s.length() <op> <value>  where op ∈ {==, !=, <, <=, >, >=}"""
    op: str       # "==", "!=", "<", "<=", ">", ">="
    value: int


# ── Regex ─────────────────────────────────────────────────────────────────────

@dataclass
class RegexConstraint(Constraint):
    """s.matches(pattern) — Java regex (anchored implicitly)"""
    pattern: str
    negated: bool = False


# ── Prefix / suffix / contains ────────────────────────────────────────────────

@dataclass
class StartsWithConstraint(Constraint):
    prefix: str
    negated: bool = False


@dataclass
class EndsWithConstraint(Constraint):
    suffix: str
    negated: bool = False


@dataclass
class ContainsConstraint(Constraint):
    substring: str
    negated: bool = False


@dataclass
class EqualsConstraint(Constraint):
    value: str
    ignore_case: bool = False
    negated: bool = False


# ── Character class ───────────────────────────────────────────────────────────

@dataclass
class CharClassConstraint(Constraint):
    """
    All characters in the string belong to a named class.
    classes: set of strings from {"alpha", "digit", "upper", "lower",
                                   "alnum", "space", "ascii_printable"}
    """
    char_class: str   # one of the above
    negated: bool = False


# ── Numeric ───────────────────────────────────────────────────────────────────

@dataclass
class ParseIntConstraint(Constraint):
    """Integer.parseInt(s) <op> <value>"""
    op: str
    value: int


@dataclass
class ParseDoubleConstraint(Constraint):
    """Double.parseDouble(s) <op> <value>"""
    op: str
    value: float


# ── Opaque fallback ───────────────────────────────────────────────────────────

@dataclass
class OpaqueConstraint(Constraint):
    """A construct we could not reduce symbolically."""
    description: str

    def is_opaque(self) -> bool:
        return True


# ── Trivial ───────────────────────────────────────────────────────────────────

@dataclass
class TrueConstraint(Constraint):
    pass


@dataclass
class FalseConstraint(Constraint):
    pass
