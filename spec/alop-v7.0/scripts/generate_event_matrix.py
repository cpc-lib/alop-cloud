#!/usr/bin/env python3
"""Generate 11-codegen/EVENT-PRODUCER-CONSUMER-MATRIX.yaml from
05-events/event-registry.yaml. This is the single derived view of the
canonical event registry; do not hand-edit the generated output."""
from pathlib import Path
import yaml

root = Path(__file__).resolve().parents[1]
registry_path = root / '05-events/event-registry.yaml'
output_path = root / '11-codegen/EVENT-PRODUCER-CONSUMER-MATRIX.yaml'

reg = yaml.safe_load(registry_path.read_text(encoding='utf-8')) or {}
events = reg.get('events', []) or []

# Build a producer -> [events] view plus the flat event list for cross-check.
matrix = {
    'version': '7.0',
    'source': '05-events/event-registry.yaml',
    'generatedBy': 'scripts/generate_event_matrix.py',
    'eventCount': len(events),
    'events': [],
    'byProducer': {},
}
for ev in events:
    et = ev.get('eventType', '')
    producer = ev.get('producer', '')
    consumers = ev.get('consumers', []) or []
    matrix['events'].append({
        'eventType': et,
        'producer': producer,
        'consumers': consumers,
    })
    matrix['byProducer'].setdefault(producer, []).append(et)

header = (
    '# GENERATED from 05-events/event-registry.yaml by scripts/generate_event_matrix.py - DO NOT EDIT\n'
    '# To change producer/consumer mappings, edit event-registry.yaml and re-run:\n'
    '#   python spec/alop-v7.0/scripts/generate_event_matrix.py\n'
)
body = yaml.safe_dump(matrix, sort_keys=False, allow_unicode=True, default_flow_style=False)
output_path.write_text(header + body, encoding='utf-8')
print(f'Generated {output_path.name}: {len(events)} events, {len(matrix["byProducer"])} producers.')
