from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re


ADVANCE_OPS = ("NOP",)

ALU_OPS = (
    "ADD",
    "SUB",
    "MUL",
    "DIV",
    "INC",
    "DEC",
    "AND",
    "OR",
    "XOR",
    "NOT",
    "CMP",
)

DATA_MOVEMENT_OPS = (
    "MOV",
    "LDACC",
    "CLACC",
    "LDX",
    "STX",
    "LDY",
    "STW",
)

CONTROL_FLOW_OPS = (
    "JMP",
    "JZ",
    "JNZ",
    "JC",
    "JNC",
    "JGT",
    "JLT",
    "CALL",
    "RET",
    "INT",
)

STACK_OPS = ("PUSHY", "POPY")

EOF_OPS = ("HALT",)

INSTRUCTION_NAMES = (
    ADVANCE_OPS
    + ALU_OPS
    + DATA_MOVEMENT_OPS
    + CONTROL_FLOW_OPS
    + STACK_OPS
    + EOF_OPS
)

DIRECTIVE_NAMES = ("DB", "INCLUDE", "ORG", "EQU", "DW", "DS")

REGISTER_NAMES = tuple(f"R{i}" for i in range(16))

IDENTIFIER_CHARS = r"A-Za-z0-9_.$"


def _alternation(names: tuple[str, ...]) -> str:
    escaped = (re.escape(name) for name in names)
    return "|".join(sorted(escaped, key=len, reverse=True))


def _keyword_group(kind: str, names: tuple[str, ...]) -> str:
    return (
        rf"(?P<{kind}>"
        rf"(?<![{IDENTIFIER_CHARS}])"
        rf"(?:{_alternation(names)})"
        rf"(?![{IDENTIFIER_CHARS}])"
        rf")"
    )


TOKEN_PATTERN = re.compile(
    rf"""
    (?P<WHITESPACE>[ \t]+)
  | (?P<COMMENT>;[^\r\n]*)
  | (?P<NEWLINE>\r\n|\r|\n)
  | (?P<STRING>"(?:\\.|[^"\\])*")
  | (?P<NUMBER>0[xX][0-9A-Fa-f]+|0[bB][01]+|0[oO][0-7]+|\d+)
  | (?P<COLON>:)
  | (?P<COMMA>,)
  | (?P<LBRACKET>\[)
  | (?P<RBRACKET>\])
  | (?P<LPAREN>\()
  | (?P<RPAREN>\))
  | (?P<PLUS>\+)
  | (?P<MINUS>-)
  | {_keyword_group("INSTRUCTION", INSTRUCTION_NAMES)}
  | {_keyword_group("DIRECTIVE", DIRECTIVE_NAMES)}
  | {_keyword_group("REGISTER", REGISTER_NAMES)}
  | (?P<IDENTIFIER>[A-Za-z_.$][A-Za-z0-9_.$]*)
  | (?P<MISMATCH>.)
    """,
    re.VERBOSE | re.IGNORECASE,
)

SKIPPED_KINDS = {"WHITESPACE", "COMMENT"}


class LexError(ValueError):
    pass


@dataclass(frozen=True, slots=True)
class Token:
    kind: str
    lexeme: str
    line: int
    column: int


class TokenStream:
    def __init__(self, tokens: list[Token]):
        self._tokens = tokens
        self._cursor = 0

    @classmethod
    def from_source(cls, source: str) -> "TokenStream":
        stream = cls([])
        stream._tokens = stream.__tokenize(source)
        return stream

    @classmethod
    def from_path(cls, path: str | Path) -> "TokenStream":
        stream = cls([])
        stream._tokens = stream.__tokenize(Path(path).read_text(encoding="utf-8"))
        return stream

    def peek(self, offset: int = 0) -> Token:
        index = min(self._cursor + offset, len(self._tokens) - 1)
        return self._tokens[index]

    def advance(self) -> Token:
        token = self.peek()
        if token.kind != "EOF":
            self._cursor += 1
        return token

    def match(self, *kinds: str) -> Token | None:
        token = self.peek()
        if token.kind in kinds:
            self._cursor += 1
            return token
        return None

    def expect(self, *kinds: str) -> Token:
        token = self.peek()
        if token.kind not in kinds:
            expected = ", ".join(kinds)
            raise LexError(
                f"Expected {expected} at line {token.line}, column {token.column}, got {token.kind}"
            )
        self._cursor += 1
        return token

    def reset(self) -> None:
        self._cursor = 0

    def remaining(self) -> list[Token]:
        return self._tokens[self._cursor :]

    def __iter__(self):
        return iter(self._tokens)

    def __len__(self) -> int:
        return len(self._tokens)

    def __tokenize(self, source: str) -> list[Token]:
        tokens: list[Token] = []
        offset = 0
        line = 1
        column = 1

        while offset < len(source):
            match = TOKEN_PATTERN.match(source, offset)
            if match is None:
                raise LexError(f"Could not tokenize line {line}, column {column}")

            kind = match.lastgroup or "MISMATCH"
            lexeme = match.group(0)

            if kind == "MISMATCH":
                raise LexError(f"Unexpected token {lexeme!r} at line {line}, column {column}")

            if kind == "NEWLINE":
                tokens.append(Token(kind, lexeme, line, column))
                offset = match.end()
                line += 1
                column = 1
                continue

            if kind not in SKIPPED_KINDS:
                tokens.append(Token(kind, lexeme, line, column))

            offset = match.end()
            column += len(lexeme)

        tokens.append(Token("EOF", "", line, column))
        return tokens
