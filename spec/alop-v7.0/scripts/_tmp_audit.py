import pathlib, re

base = pathlib.Path('spec/alop-v7.0/04-openapi')

# find amount-like property names declared as number
name_pat = re.compile(r'amount|price|fee|rent|deposit|charge|balance|total|deduct|refund|payment', re.I)

for f in sorted(base.glob('*.yaml')):
    text = f.read_text(encoding='utf-8')
    lines = text.splitlines()
    # naive state machine: track current property name and its following schema block
    current = None
    depth = 0
    for i, line in enumerate(lines):
        m = re.match(r'^(\s+)([A-Za-z0-9_]+):\s*$', line)
        if m:
            name = m.group(2)
            if name_pat.match(name):
                current = (name, i + 1)
                continue
        if current:
            if 'type:' in line and 'number' in line:
                print(f.stem, 'line', current[1], current[0], '->', line.strip())
                current = None
            elif re.match(r'^\s{0,10}\S', line) and ':' in line and not line.startswith(' ' * 11):
                pass
        # reset current when a new property starts
        m2 = re.match(r'^(\s+)([A-Za-z0-9_]+):', line)
        if m2 and m2.group(1) == '        ' and current:
            current = None
