#!/usr/bin/env python3
from pathlib import Path
import yaml, json, re, collections, sys, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
errors=[]
# YAML
for p in root.rglob('*.yaml'):
    try: yaml.safe_load(p.read_text(encoding='utf-8'))
    except Exception as e: errors.append(f'YAML parse: {p.relative_to(root)}: {e}')
# JSON
for p in root.rglob('*.json'):
    try: json.loads(p.read_text(encoding='utf-8'))
    except Exception as e: errors.append(f'JSON parse: {p.relative_to(root)}: {e}')
# BPMN/XML
for p in root.rglob('*.xml'):
    try: ET.parse(p)
    except Exception as e: errors.append(f'XML parse: {p.relative_to(root)}: {e}')
# OpenAPI structural checks
opids=collections.defaultdict(list); op_count=0
for p in (root/'04-openapi').glob('*.yaml'):
    d=yaml.safe_load(p.read_text(encoding='utf-8')) or {}
    if d.get('openapi')!='3.0.3': errors.append(f'OpenAPI version: {p.name}')
    for path,item in (d.get('paths') or {}).items():
        if not isinstance(item,dict): continue
        for method,op in item.items():
            if method.lower() not in {'get','post','put','patch','delete'} or not isinstance(op,dict): continue
            op_count += 1
            oid=op.get('operationId')
            if not oid: errors.append(f'operationId missing: {p.name} {method.upper()} {path}')
            else: opids[oid].append(f'{p.name}:{method}:{path}')
            placeholders=set(re.findall(r'{([^}]+)}',path)); declared=set()
            for prm in (item.get('parameters',[]) or [])+(op.get('parameters',[]) or []):
                if isinstance(prm,dict) and prm.get('in')=='path':
                    declared.add(prm.get('name'))
                    if prm.get('required') is not True: errors.append(f'path parameter not required: {p.name} {path} {prm.get("name")}')
            if placeholders-declared: errors.append(f'path parameter missing: {p.name} {path} {sorted(placeholders-declared)}')
for oid,locs in opids.items():
    if len(locs)>1: errors.append(f'duplicate operationId {oid}: {locs}')
# Event schemas
for p in (root/'05-events/schemas').glob('*.json'):
    d=json.loads(p.read_text(encoding='utf-8'))
    props=d.get('properties',{})
    if 'eventType' not in props or 'eventVersion' not in props: errors.append(f'event envelope incomplete: {p.name}')
# Task references
for p in (root/'tasks').glob('TASK-*.md'):
    text=p.read_text(encoding='utf-8')
    if 'MASTER-SPEC-V7.0.md' not in text: errors.append(f'V7 master missing: {p.name}')
    if 'MASTER-SPEC-V6' in text: errors.append(f'stale V6 master: {p.name}')
# DDL duplicate table in same schema module
tables=collections.defaultdict(list); table_count=0
for p in root.glob('03-database/flyway/*/*.sql'):
    for table in re.findall(r'CREATE\s+TABLE\s+`?([A-Za-z0-9_]+)`?',p.read_text(encoding='utf-8'),re.I):
        tables[(p.parent.name,table)].append(p.name); table_count += 1
for k,v in tables.items():
    if len(v)>1: errors.append(f'duplicate CREATE TABLE in module {k}: {v}')
# Required codegen controls
required=[
 'SERVICE-MODULE-MATRIX.yaml','TRANSACTION-LOCK-MATRIX.yaml','IDEMPOTENCY-MATRIX.yaml','JOB-MATRIX.yaml',
 'state-machines.yaml','TASK-CONTEXT-MATRIX.yaml','EVENT-PRODUCER-CONSUMER-MATRIX.yaml','API-CATALOG.yaml',
 'TRACEABILITY-MATRIX.csv'
]
for f in required:
    if not (root/'11-codegen'/f).exists(): errors.append(f'missing codegen control: {f}')
# Task bundle coverage
tasks=list((root/'tasks').glob('TASK-*.md'))
for p in tasks:
    if not (root/'14-task-bundles'/p.stem/'CONTEXT.md').exists(): errors.append(f'missing task bundle: {p.stem}')
print(f'Validated: {op_count} API operations, {table_count} DDL tables, {len(list((root/"05-events/schemas").glob("*.json")))} event schemas, {len(tasks)} tasks.')
if errors:
    print(f'FAILED: {len(errors)} issue(s)')
    for e in errors: print(' -',e)
    sys.exit(1)
print('PASS: V7.0 structural validation successful.')
