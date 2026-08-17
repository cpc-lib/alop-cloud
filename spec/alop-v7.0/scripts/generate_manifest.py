#!/usr/bin/env python3
"""V7.0 spec generator.

Three responsibilities:
1. MANIFEST.md — sha256 hash list of every spec file.
2. 03-database/TABLE-CATALOG.yaml — machine-readable table catalog parsed
   directly from the Flyway migration SQL.
3. 03-database/DATA-DICTIONARY.md — human-readable rendering of the same
   catalog, grouped by migration module.

The DDL parser is intentionally a small recursive-descent scanner. It must
correctly handle:
 * single-line and multi-line CREATE TABLE statements,
 * comma-separated column lists on a single line (the previous parser only
   took the first column of every line — see the tenant package tables for
   the breakage this caused),
 * parenthesised type arguments such as DECIMAL(20,6) and VARCHAR(64) which
   contain commas that MUST NOT be treated as column separators,
 * inline PRIMARY KEY markers on a column (e.g. `id BIGINT PRIMARY KEY`),
 * separate PRIMARY KEY / UNIQUE KEY / KEY / INDEX clauses,
 * CONSTRAINT ... FOREIGN KEY / CHECK clauses (skipped — not part of the
   catalog surface).

Run from the repository root:

    python spec/alop-v7.0/scripts/generate_manifest.py
"""
from __future__ import annotations

import hashlib
import re
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
DB_DIR = ROOT / "03-database"
FLYWAY_DIR = DB_DIR / "flyway"
CATALOG_PATH = DB_DIR / "TABLE-CATALOG.yaml"
DATA_DICT_PATH = DB_DIR / "DATA-DICTIONARY.md"


# ---------------------------------------------------------------------------
# MANIFEST.md (preserved from the previous script)
# ---------------------------------------------------------------------------
def write_manifest() -> int:
    manifest = ROOT / "MANIFEST.md"
    if manifest.exists():
        manifest.unlink()
    files = sorted(p for p in ROOT.rglob("*") if p.is_file() and p != manifest)
    lines = [
        "# ALOP-SaaS V7.0 MANIFEST",
        "",
        f"Total files (excluding MANIFEST): {len(files)}",
        "",
        "## Files",
        "",
    ]
    for p in files:
        h = hashlib.sha256(p.read_bytes()).hexdigest()[:16]
        lines.append(f"- `{p.relative_to(ROOT)}` — sha256:{h}")
    manifest.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return len(files)


# ---------------------------------------------------------------------------
# SQL lexer helpers
# ---------------------------------------------------------------------------
def strip_sql_comments(text: str) -> str:
    """Strip /* block */ and -- line comments. String literals are preserved."""
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    out_lines = []
    for line in text.split("\n"):
        in_str = False
        i = 0
        result = []
        while i < len(line):
            c = line[i]
            if c == "'":
                in_str = not in_str
                result.append(c)
                i += 1
                continue
            if c == "-" and i + 1 < len(line) and line[i + 1] == "-" and not in_str:
                break
            result.append(c)
            i += 1
        out_lines.append("".join(result))
    return "\n".join(out_lines)


def find_matching_paren(text: str, open_index: int) -> int:
    """Given the index of an opening '(' return the index of its matching ')'.

    String literals and nested parentheses are tracked.
    Returns -1 if no match is found.
    """
    depth = 1
    i = open_index + 1
    in_str = False
    while i < len(text):
        c = text[i]
        if c == "'":
            in_str = not in_str
        elif not in_str:
            if c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    return -1


def split_top_level(body: str) -> list[str]:
    """Split a CREATE TABLE body on commas at paren depth 0.

    String literals are respected so commas inside quoted defaults do not
    split. This is what fixes the previous parser: a single line such as
    `id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, ...` now yields one
    entry per column instead of just the first.
    """
    parts: list[str] = []
    depth = 0
    in_str = False
    cur: list[str] = []
    for c in body:
        if c == "'":
            in_str = not in_str
            cur.append(c)
            continue
        if in_str:
            cur.append(c)
            continue
        if c == "(":
            depth += 1
            cur.append(c)
        elif c == ")":
            depth -= 1
            cur.append(c)
        elif c == "," and depth == 0:
            piece = "".join(cur).strip()
            if piece:
                parts.append(piece)
            cur = []
        else:
            cur.append(c)
    last = "".join(cur).strip()
    if last:
        parts.append(last)
    return [p for p in parts if p]


# ---------------------------------------------------------------------------
# CREATE TABLE discovery
# ---------------------------------------------------------------------------
_CREATE_RE = re.compile(
    r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(?P<name>[A-Za-z_][A-Za-z0-9_]*)`?\s*\(",
    re.IGNORECASE,
)


def iter_create_tables(text: str):
    """Yield (table_name, body) for every CREATE TABLE in *text*."""
    for m in _CREATE_RE.finditer(text):
        body_start = m.end()  # right after the opening '('
        close = find_matching_paren(text, m.end() - 1)
        if close == -1:
            continue
        yield m.group("name"), text[body_start:close]


# ---------------------------------------------------------------------------
# Piece classifiers
# ---------------------------------------------------------------------------
_INDEX_LEADERS = {
    "PRIMARY",
    "UNIQUE",
    "FULLTEXT",
    "SPATIAL",
    "KEY",
    "INDEX",
    "CONSTRAINT",
    "FOREIGN",
    "CHECK",
}

_PRIMARY_RE = re.compile(
    r"PRIMARY\s+KEY\s*`?(?P<name>[A-Za-z0-9_]*)`?\s*\((?P<cols>[^)]*)\)",
    re.IGNORECASE,
)
_UNIQUE_RE = re.compile(
    r"UNIQUE\s+(?:KEY|INDEX)?\s*`?(?P<name>[A-Za-z0-9_]*)`?\s*\((?P<cols>[^)]*)\)",
    re.IGNORECASE,
)
_SPECIAL_RE = re.compile(
    r"(?P<kind>FULLTEXT|SPATIAL)\s+(?:KEY|INDEX)?\s*`?(?P<name>[A-Za-z0-9_]*)`?\s*\((?P<cols>[^)]*)\)",
    re.IGNORECASE,
)
_KEY_RE = re.compile(
    r"(?:KEY|INDEX)\s*`?(?P<name>[A-Za-z0-9_]*)`?\s*\((?P<cols>[^)]*)\)",
    re.IGNORECASE,
)


def _clean_cols(raw: str) -> str:
    """Normalise a column list: split on commas, strip whitespace and backticks."""
    cols = [c.strip().strip("`") for c in raw.split(",") if c.strip()]
    return ", ".join(cols)


def parse_index(piece: str):
    """Parse an index/constraint clause. Returns a dict or None (to skip)."""
    s = piece.strip()
    head = s.split(None, 1)[0].upper() if s.split() else ""
    # CONSTRAINT name FOREIGN KEY ... / CONSTRAINT name CHECK (...) — skipped.
    if head == "CONSTRAINT":
        # Drop the constraint name token if present, then look at the next token.
        tokens = s.split()
        if len(tokens) >= 2 and tokens[1].upper() in ("FOREIGN", "CHECK"):
            return None
        if len(tokens) >= 2:
            # CONSTRAINT <name> FOREIGN KEY ... or CONSTRAINT <name> CHECK ...
            second = tokens[1].upper()
            if second in ("PRIMARY", "UNIQUE", "FOREIGN", "CHECK"):
                # Re-parse the remainder after the constraint name.
                remainder = " ".join(tokens[2:])
                if second == "PRIMARY":
                    m = _PRIMARY_RE.match(remainder)
                    if m:
                        return {
                            "type": "PRIMARY KEY",
                            "name": m.group("name") or None,
                            "columns": _clean_cols(m.group("cols")),
                        }
                elif second == "UNIQUE":
                    m = _UNIQUE_RE.match(remainder)
                    if m:
                        return {
                            "type": "UNIQUE KEY",
                            "name": m.group("name") or None,
                            "columns": _clean_cols(m.group("cols")),
                        }
        return None
    if head == "FOREIGN" or head == "CHECK":
        return None

    m = _PRIMARY_RE.match(s)
    if m:
        return {
            "type": "PRIMARY KEY",
            "name": m.group("name") or None,
            "columns": _clean_cols(m.group("cols")),
        }
    m = _UNIQUE_RE.match(s)
    if m:
        return {
            "type": "UNIQUE KEY",
            "name": m.group("name") or None,
            "columns": _clean_cols(m.group("cols")),
        }
    m = _SPECIAL_RE.match(s)
    if m:
        return {
            "type": m.group("kind").upper() + " KEY",
            "name": m.group("name") or None,
            "columns": _clean_cols(m.group("cols")),
        }
    m = _KEY_RE.match(s)
    if m:
        return {
            "type": "KEY",
            "name": m.group("name") or None,
            "columns": _clean_cols(m.group("cols")),
        }
    return None


_TYPE_RE = re.compile(r"([A-Za-z]+)\s*(\(([^)]*)\))?")
_DEFAULT_RE = re.compile(
    r"\bDEFAULT\s+(?P<val>'[^']*'|\"[^\"]*\"|[^\s,]+)",
    re.IGNORECASE,
)
_NOT_NULL_RE = re.compile(r"\bNOT\s+NULL\b", re.IGNORECASE)
_NULL_RE = re.compile(r"(?<!\bNOT\s)\bNULL\b", re.IGNORECASE)
_INLINE_PK_RE = re.compile(r"\bPRIMARY\s+KEY\b", re.IGNORECASE)


def parse_column(piece: str):
    """Parse a column definition. Returns a dict or None on parse failure."""
    s = piece.strip()
    if not s:
        return None
    tokens = s.split(None, 1)
    if not tokens:
        return None
    name_tok = tokens[0]
    name = name_tok.strip("`")
    rest = tokens[1] if len(tokens) > 1 else ""

    m = _TYPE_RE.match(rest)
    if not m:
        # Column with no type? Skip rather than emit garbage.
        return None
    type_name = m.group(1).upper()
    type_args = m.group(2) or ""
    # Normalise "DECIMAL( 20 , 6 )" -> "DECIMAL(20,6)"
    type_args = re.sub(r"\s+", "", type_args)
    col_type = f"{type_name}{type_args}" if type_args else type_name
    remainder = rest[m.end():].strip()

    inline_pk = bool(_INLINE_PK_RE.search(remainder))
    not_null = bool(_NOT_NULL_RE.search(remainder))
    explicit_null = bool(_NULL_RE.search(remainder))

    # MySQL: a column is nullable unless NOT NULL or inline PRIMARY KEY.
    nullable = not (not_null or inline_pk)
    # Explicit "NULL" without "NOT NULL" is just a no-op marker; keep nullable True.
    _ = explicit_null

    default = None
    dm = _DEFAULT_RE.search(remainder)
    if dm:
        default = dm.group("val")

    return {
        "name": name,
        "type": col_type,
        "nullable": nullable,
        "default": default,
        "inline_pk": inline_pk,
    }


def classify_piece(piece: str):
    """Return ('column', dict) or ('index', dict) or ('skip', None)."""
    head = piece.split(None, 1)[0].upper() if piece.split() else ""
    if head in _INDEX_LEADERS:
        idx = parse_index(piece)
        return ("index", idx) if idx else ("skip", None)
    return ("column", parse_column(piece))


# ---------------------------------------------------------------------------
# Table parsing
# ---------------------------------------------------------------------------
def parse_sql_file(path: Path) -> list[dict]:
    """Return a list of {table, columns, indexes} dicts parsed from *path*."""
    text = strip_sql_comments(path.read_text(encoding="utf-8"))
    out: list[dict] = []
    for table_name, body in iter_create_tables(text):
        columns: list[dict] = []
        indexes: list[dict] = []
        for piece in split_top_level(body):
            kind, parsed = classify_piece(piece)
            if kind == "column" and parsed:
                columns.append(parsed)
                if parsed.get("inline_pk"):
                    indexes.append(
                        {
                            "type": "PRIMARY KEY",
                            "name": None,
                            "columns": parsed["name"],
                        }
                    )
            elif kind == "index" and parsed:
                indexes.append(parsed)
        out.append(
            {
                "table": table_name,
                "columns": [
                    {
                        "name": c["name"],
                        "type": c["type"],
                        "nullable": c["nullable"],
                        "default": c["default"],
                    }
                    for c in columns
                ],
                "indexes": indexes,
            }
        )
    return out


# ---------------------------------------------------------------------------
# YAML catalog
# ---------------------------------------------------------------------------
class _CatalogDumper(yaml.SafeDumper):
    """Use safe dumping with deterministic flow style off and no key sorting."""


def _none_representer(dumper, _data):
    # Render python None as `null` (PyYAML's default safe representer already
    # does this; the subclass is only used to keep flow_style=False).
    return dumper.represent_scalar("tag:yaml.org,2002:null", "null")


_CatalogDumper.add_representer(type(None), _none_representer)


def to_catalog_entry(module: str, rel_migration: str, table: dict) -> dict:
    return {
        "module": module,
        "migration": rel_migration,
        "table": table["table"],
        "columns": [
            {
                "name": c["name"],
                "type": c["type"],
                "nullable": c["nullable"],
                "default": c["default"],
            }
            for c in table["columns"]
        ],
        "indexes": [
            {"type": i["type"], "name": i["name"], "columns": i["columns"]}
            for i in table["indexes"]
        ],
    }


def write_catalog(catalog: list[dict]) -> None:
    doc = {"version": "7.0", "tables": catalog}
    text = yaml.dump(
        doc,
        Dumper=_CatalogDumper,
        default_flow_style=False,
        sort_keys=False,
        allow_unicode=True,
        width=4096,
    )
    CATALOG_PATH.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Markdown data dictionary
# ---------------------------------------------------------------------------
def _md_default(val) -> str:
    if val is None:
        return "`None`"
    return f"`{val}`"


def _md_nullable(val: bool) -> str:
    return "NO" if not val else "YES"


def _md_index_line(idx: dict) -> str:
    name = idx["name"]
    cols = idx["columns"]
    if idx["type"] == "PRIMARY KEY":
        return f"- PRIMARY KEY `` ({cols})"
    return f"- {idx['type']} `{name}` ({cols})"


def write_data_dictionary(catalog_by_module: dict[str, list[dict]]) -> int:
    total_tables = sum(len(tables) for tables in catalog_by_module.values())
    lines = [
        "# Database Data Dictionary — V7.0",
        "",
        f"Total parsed tables: **{total_tables}**.",
        "",
        "> Canonical executable schema remains the Flyway SQL. This document is generated for code review/codegen navigation.",
        "",
    ]
    for module in sorted(catalog_by_module):
        lines.append(f"## {module}")
        lines.append("")
        for entry in catalog_by_module[module]:
            lines.append(f"### `{entry['table']}`")
            lines.append("")
            lines.append(f"Migration: `{entry['migration']}`")
            lines.append("")
            lines.append("| Column | Type | Nullable | Default |")
            lines.append("|---|---|---:|---|")
            for c in entry["columns"]:
                lines.append(
                    f"| `{c['name']}` | `{c['type']}` | "
                    f"{_md_nullable(c['nullable'])} | {_md_default(c['default'])} |"
                )
            if entry["indexes"]:
                lines.append("")
                lines.append("Indexes:")
                for idx in entry["indexes"]:
                    lines.append(_md_index_line(idx))
            lines.append("")
    DATA_DICT_PATH.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
    return total_tables


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def build_catalog() -> tuple[list[dict], dict[str, list[dict]]]:
    catalog: list[dict] = []
    by_module: dict[str, list[dict]] = {}
    for sql_path in sorted(FLYWAY_DIR.glob("*/*.sql")):
        module = sql_path.parent.name
        rel = f"03-database/flyway/{module}/{sql_path.name}"
        for table in parse_sql_file(sql_path):
            entry = to_catalog_entry(module, rel, table)
            catalog.append(entry)
            by_module.setdefault(module, []).append(entry)
    return catalog, by_module


def main() -> None:
    file_count = write_manifest()
    print(f"Wrote MANIFEST.md with {file_count} files")

    catalog, by_module = build_catalog()
    write_catalog(catalog)
    total = write_data_dictionary(by_module)
    print(
        f"Wrote {CATALOG_PATH.relative_to(ROOT)} and "
        f"{DATA_DICT_PATH.relative_to(ROOT)} with {len(catalog)} tables "
        f"({sum(len(e['columns']) for e in catalog)} columns, "
        f"{sum(len(e['indexes']) for e in catalog)} indexes)."
    )


if __name__ == "__main__":
    main()
