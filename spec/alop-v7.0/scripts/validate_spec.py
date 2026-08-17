#!/usr/bin/env python3
"""ALOP V7.0 spec structural + semantic validator.

Checks:
  1.  YAML / JSON / XML parseability
  2.  OpenAPI 3.0.3 + operationId uniqueness
  3.  Path parameter bidirectional check (placeholder<->declared)
  4.  Path segment ASCII whitelist (detects homoglyph pollution)
  5.  Money field type enforcement (no number/double/float for money)
  6.  DDL precision assertion (money 18,2 / quantity 20,6 / rate 20,8)
  7.  Event registry <-> lock/idempotency matrix successEvent cross-check
  8.  Idempotency/lock matrix operationId exists in OpenAPI/CATALOG
  9.  state-machines.yaml machine registered in STATE-MACHINE-CATALOG.md
  10. EVENT-PRODUCER-CONSUMER-MATRIX derives from event-registry
  11. Task bundle CONTEXT.md referenced paths exist
  12. Required codegen controls present
  13. Task references MASTER-SPEC-V7.0 (not V6)
  14. DDL duplicate table in same module
  15. Event schema envelope completeness
"""
from pathlib import Path
import yaml, json, re, collections, sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
errors = []
warnings = []

# ---- money field classification -------------------------------------------
MONEY_KEYWORDS = (
    'amount', 'price', 'fee', 'balance', 'total', 'paid', 'refund', 'charge',
    'deposit', 'rent', 'penalty', 'deduction', 'net', 'gross', 'tax',
    'outstanding', 'payable', 'receivable', 'settlement', 'quota', 'advance',
    'liability', 'cost', 'income', 'revenue', 'principal',
)
NON_MONEY_KEYWORDS = (
    'rate', 'ratio', 'multiplier', 'quantity', 'count', 'version', 'size',
    'page', 'limit', 'offset', 'duration', 'minute', 'hour', 'day', 'percent',
    'percentage', 'index', 'level', 'tier', 'step', 'sequence', 'order',
)

def is_money_field(name):
    n = name.lower()
    # split on underscores/camel boundaries for accurate keyword matching
    tokens = re.split(r'[_\s]+|(?<=[a-z])(?=[A-Z])', n)
    tokens = [t for t in tokens if t]
    if any(k in n for k in NON_MONEY_KEYWORDS):
        return False
    # match on token boundaries to avoid false positives (e.g. 'reading' containing 'rent')
    for tok in tokens:
        if tok in MONEY_KEYWORDS:
            return True
    return False

def walk_schema(node, path_str, visitor):
    """Recursively visit every property schema in a JSON Schema node."""
    if not isinstance(node, dict):
        return
    props = node.get('properties') or {}
    for fname, fschema in props.items():
        if isinstance(fschema, dict):
            visitor(fname, fschema, f'{path_str}.{fname}')
            walk_schema(fschema, f'{path_str}.{fname}', visitor)
        for key in ('items', 'additionalProperties'):
            child = node.get(key)
            if isinstance(child, dict):
                walk_schema(child, f'{path_str}.{key}', visitor)
    for key in ('allOf', 'oneOf', 'anyOf'):
        arr = node.get(key)
        if isinstance(arr, list):
            for i, item in enumerate(arr):
                if isinstance(item, dict):
                    walk_schema(item, f'{path_str}.{key}[{i}]', visitor)

# ---- 1. parseability ------------------------------------------------------
for p in root.rglob('*.yaml'):
    try:
        yaml.safe_load(p.read_text(encoding='utf-8'))
    except Exception as e:
        errors.append(f'YAML parse: {p.relative_to(root)}: {e}')
for p in root.rglob('*.json'):
    try:
        json.loads(p.read_text(encoding='utf-8'))
    except Exception as e:
        errors.append(f'JSON parse: {p.relative_to(root)}: {e}')
for p in root.rglob('*.xml'):
    try:
        ET.parse(p)
    except Exception as e:
        errors.append(f'XML parse: {p.relative_to(root)}: {e}')

# ---- 2-4. OpenAPI checks --------------------------------------------------
opids = collections.defaultdict(list)
op_count = 0
all_opids = set()
openapi_files = sorted((root / '04-openapi').glob('*.yaml'))

for p in openapi_files:
    d = yaml.safe_load(p.read_text(encoding='utf-8')) or {}
    if d.get('openapi') != '3.0.3':
        errors.append(f'OpenAPI version: {p.name}')
    for path, item in (d.get('paths') or {}).items():
        if not isinstance(item, dict):
            continue
        # 4. path segment ASCII whitelist
        for seg in path.split('/'):
            if seg and not re.match(r'^[A-Za-z0-9_{}.\-]+$', seg):
                errors.append(f'non-ASCII/illegal char in path segment: {p.name} {path} segment={seg!r}')
        for method, op in item.items():
            if method.lower() not in {'get', 'post', 'put', 'patch', 'delete'} or not isinstance(op, dict):
                continue
            op_count += 1
            oid = op.get('operationId')
            if not oid:
                errors.append(f'operationId missing: {p.name} {method.upper()} {path}')
            else:
                opids[oid].append(f'{p.name}:{method}:{path}')
                all_opids.add(oid)
            # 3. bidirectional path parameter check
            placeholders = set(re.findall(r'{([^}]+)}', path))
            declared = set()
            for prm in (item.get('parameters', []) or []) + (op.get('parameters', []) or []):
                if isinstance(prm, dict) and prm.get('in') == 'path':
                    declared.add(prm.get('name'))
                    if prm.get('required') is not True:
                        errors.append(f'path parameter not required: {p.name} {path} {prm.get("name")}')
            missing_decl = placeholders - declared
            if missing_decl:
                errors.append(f'path placeholder undeclared: {p.name} {path} {sorted(missing_decl)}')
            orphan_decl = declared - placeholders
            if orphan_decl:
                errors.append(f'path parameter declared but not in path: {p.name} {path} {sorted(orphan_decl)}')
    # 5. money field type enforcement
    def money_visitor(fname, fschema, fpath):
        ftype = fschema.get('type')
        fmt = fschema.get('format')
        is_money = is_money_field(fname)
        if fmt in ('double', 'float') and is_money:
            errors.append(f'float/double forbidden for money: {p.name} {fpath} (format={fmt})')
        if ftype == 'number' and is_money:
            errors.append(f'money field must be string+pattern: {p.name} {fpath} (got type=number)')
        if fmt in ('double', 'float') and not is_money:
            warnings.append(f'float/double discouraged for non-money: {p.name} {fpath} (use string or integer)')
        if fmt == 'decimal':
            warnings.append(f'non-standard format decimal: {p.name} {fpath} (use string+pattern)')
    for sname, schema in (d.get('components', {}).get('schemas', {}) or {}).items():
        if isinstance(schema, dict):
            walk_schema(schema, sname, money_visitor)

for oid, locs in opids.items():
    if len(locs) > 1:
        errors.append(f'duplicate operationId {oid}: {locs}')

# ---- 6. DDL precision assertion -------------------------------------------
RATE_KEYWORDS = ('rate', 'ratio', 'multiplier', 'tariff', 'unit_price', 'allocation_ratio')
QUANTITY_KEYWORDS = ('reading', 'consumption', 'usage', 'quantity', 'volume', 'start_value', 'end_value', 'raw_usage', 'billable_usage', 'chargeable_area', 'charge_basis')
DDL_FILES = sorted(root.glob('03-database/flyway/*/*.sql'))
for p in DDL_FILES:
    text = p.read_text(encoding='utf-8')
    for m in re.finditer(r'([A-Za-z0-9_]+)\s+DECIMAL\s*\(\s*(\d+)\s*,\s*(\d+)\s*\)', text, re.I):
        col, prec, scale = m.group(1).lower(), int(m.group(2)), int(m.group(3))
        if any(k in col for k in RATE_KEYWORDS):
            if (prec, scale) != (20, 8):
                errors.append(f'rate precision: {p.name} {col} DECIMAL({prec},{scale}) expected (20,8)')
        elif any(k in col for k in QUANTITY_KEYWORDS):
            if (prec, scale) != (20, 6):
                errors.append(f'quantity precision: {p.name} {col} DECIMAL({prec},{scale}) expected (20,6)')
        elif is_money_field(col):
            if (prec, scale) != (18, 2):
                errors.append(f'money precision: {p.name} {col} DECIMAL({prec},{scale}) expected (18,2)')

# ---- 14. DDL duplicate table in same module --------------------------------
tables = collections.defaultdict(list)
table_count = 0
for p in DDL_FILES:
    for table in re.findall(r'CREATE\s+TABLE\s+`?([A-Za-z0-9_]+)`?', p.read_text(encoding='utf-8'), re.I):
        tables[(p.parent.name, table)].append(p.name)
        table_count += 1
for k, v in tables.items():
    if len(v) > 1:
        errors.append(f'duplicate CREATE TABLE in module {k}: {v}')

# ---- 15. event schema envelope --------------------------------------------
for p in sorted((root / '05-events/schemas').glob('*.json')):
    d = json.loads(p.read_text(encoding='utf-8'))
    props = d.get('properties', {})
    if 'eventType' not in props or 'eventVersion' not in props:
        errors.append(f'event envelope incomplete: {p.name}')

# ---- 7. event registry <-> matrix successEvent ----------------------------
registry_path = root / '05-events/event-registry.yaml'
event_types = set()
if registry_path.exists():
    reg = yaml.safe_load(registry_path.read_text(encoding='utf-8')) or {}
    for ev in reg.get('events', []) or []:
        et = ev.get('eventType')
        if et:
            event_types.add(et)
        else:
            errors.append(f'event-registry entry missing eventType: {ev}')

for matrix_file in ('TRANSACTION-LOCK-MATRIX.yaml', 'IDEMPOTENCY-MATRIX.yaml'):
    mp = root / '11-codegen' / matrix_file
    if not mp.exists():
        continue
    mat = yaml.safe_load(mp.read_text(encoding='utf-8')) or {}
    def scan_matrix_entries(obj, ctx=''):
        if isinstance(obj, dict):
            se = obj.get('successEvent')
            if se and se != 'internal-only' and se not in event_types:
                errors.append(f'{matrix_file} successEvent not in registry: {se} ({ctx})')
            oid = obj.get('operationId')
            if oid and oid not in all_opids and not oid.startswith('TODO'):
                errors.append(f'{matrix_file} operationId not found in OpenAPI: {oid} ({ctx})')
            for k, v in obj.items():
                scan_matrix_entries(v, f'{ctx}.{k}' if ctx else k)
        elif isinstance(obj, list):
            for i, item in enumerate(obj):
                scan_matrix_entries(item, f'{ctx}[{i}]')
    scan_matrix_entries(mat)

# ---- 8. API-CATALOG operationId coverage ----------------------------------
catalog_path = root / '11-codegen/API-CATALOG.yaml'
if catalog_path.exists():
    cat_text = catalog_path.read_text(encoding='utf-8')
    catalog_opids = set(re.findall(r'operationId:\s*([A-Za-z0-9_]+)', cat_text))
    orphan = catalog_opids - all_opids
    if orphan:
        errors.append(f'API-CATALOG operationId not in OpenAPI: {sorted(orphan)}')

# ---- 9. state-machines.yaml registered in CATALOG --------------------------
sm_path = root / '11-codegen/state-machines.yaml'
cat_md_path = root / '11-codegen/STATE-MACHINE-CATALOG.md'
if sm_path.exists() and cat_md_path.exists():
    sm = yaml.safe_load(sm_path.read_text(encoding='utf-8')) or {}
    machines = sm.get('machines', {}) or {}
    cat_text = cat_md_path.read_text(encoding='utf-8')
    for mname in machines:
        if mname not in cat_text:
            errors.append(f'state machine not registered in CATALOG: {mname}')

# ---- 10. EVENT-PRODUCER-CONSUMER-MATRIX derives from registry --------------
epcm_path = root / '11-codegen/EVENT-PRODUCER-CONSUMER-MATRIX.yaml'
if epcm_path.exists() and registry_path.exists():
    epcm_text = epcm_path.read_text(encoding='utf-8')
    if 'GENERATED' not in epcm_text and 'generated' not in epcm_text.lower():
        warnings.append('EVENT-PRODUCER-CONSUMER-MATRIX.yaml missing GENERATED marker (should derive from event-registry)')
    epcm = yaml.safe_load(epcm_text) if epcm_path.exists() else {}
    def collect_event_types(obj, acc):
        if isinstance(obj, dict):
            et = obj.get('eventType')
            if et:
                acc.add(et)
            for v in obj.values():
                collect_event_types(v, acc)
        elif isinstance(obj, list):
            for item in obj:
                collect_event_types(item, acc)
    epcm_types = set()
    collect_event_types(epcm, epcm_types)
    matrix_only = epcm_types - event_types
    if matrix_only:
        errors.append(f'EVENT-PRODUCER-CONSUMER-MATRIX eventType not in registry: {sorted(matrix_only)}')

# ---- 11. task bundle referenced paths exist --------------------------------
project_root = root.parents[1]  # d:\code\specs\alop-cloud
for p in sorted((root / '14-task-bundles').glob('TASK-*/CONTEXT.md')):
    text = p.read_text(encoding='utf-8')
    for m in re.finditer(r'`([0-9A-Za-z_\-/]+(?:\.[A-Za-z]+))`', text):
        ref = m.group(1)
        if ref.startswith('http') or ref.startswith('spec/'):
            continue
        if any(ref.startswith(d) for d in ('MASTER', 'V7', 'CODEGEN', 'CODING')):
            continue
        if '/' not in ref or '.' not in ref:
            continue
        exists = (root / ref).exists() or (project_root / ref).exists()
        if not exists:
            errors.append(f'task bundle dangling ref: {p.parent.name} -> {ref}')

# ---- 12. required codegen controls ----------------------------------------
required = [
    'SERVICE-MODULE-MATRIX.yaml', 'TRANSACTION-LOCK-MATRIX.yaml',
    'IDEMPOTENCY-MATRIX.yaml', 'JOB-MATRIX.yaml', 'state-machines.yaml',
    'TASK-CONTEXT-MATRIX.yaml', 'EVENT-PRODUCER-CONSUMER-MATRIX.yaml',
    'API-CATALOG.yaml', 'TRACEABILITY-MATRIX.csv',
]
for f in required:
    if not (root / '11-codegen' / f).exists():
        errors.append(f'missing codegen control: {f}')

# ---- 13. task references --------------------------------------------------
tasks = sorted((root / 'tasks').glob('TASK-*.md'))
for p in tasks:
    text = p.read_text(encoding='utf-8')
    if 'MASTER-SPEC-V7.0.md' not in text:
        errors.append(f'V7 master missing: {p.name}')
    if 'MASTER-SPEC-V6' in text:
        errors.append(f'stale V6 master: {p.name}')
    if not (root / '14-task-bundles' / p.stem / 'CONTEXT.md').exists():
        errors.append(f'missing task bundle: {p.stem}')

# ---- summary --------------------------------------------------------------
print(f'Validated: {op_count} API operations, {table_count} DDL tables, '
      f'{len(list((root / "05-events/schemas").glob("*.json")))} event schemas, {len(tasks)} tasks.')
if warnings:
    print(f'WARNINGS: {len(warnings)}')
    for w in warnings:
        print('  W:', w)
if errors:
    print(f'FAILED: {len(errors)} issue(s)')
    for e in errors:
        print('  -', e)
    sys.exit(1)
print('PASS: V7.0 structural + semantic validation successful.')
