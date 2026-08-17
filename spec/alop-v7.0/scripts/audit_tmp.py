import pathlib, re

out = []
base = pathlib.Path(__file__).resolve().parents[1] / '04-openapi'
files = sorted(base.glob('*.y*ml'))
out.append('FILES ' + ', '.join(f.name for f in files))

name_pat = re.compile(r'amount|price|fee|rent|deposit|charge|balance|total|deduct|refund', re.I)

for f in files:
    lines = f.read_text(encoding='utf-8').splitlines()
    current = None
    for i, line in enumerate(lines):
        m = re.match(r'^(\s+)([A-Za-z0-9_]+):\s*$', line)
        if m and name_pat.search(m.group(2)):
            current = (m.group(2), i + 1)
            continue
        if current:
            if 'number' in line and 'type:' in line:
                out.append(f'{f.stem} line {current[1]} {current[0]} -> {line.strip()}')
                current = None
        m2 = re.match(r'^(\s+)([A-Za-z0-9_]+):', line)
        if m2 and current and m2.group(1) == '        ':
            current = None

(pathlib.Path(__file__).parent / '_audit_result.txt').write_text('\n'.join(out), encoding='utf-8')
