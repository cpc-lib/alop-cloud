#!/usr/bin/env python3
from pathlib import Path
import hashlib
root=Path(__file__).resolve().parents[1]
manifest=root/'MANIFEST.md'
if manifest.exists(): manifest.unlink()
files=sorted(p for p in root.rglob('*') if p.is_file() and p!=manifest)
lines=['# ALOP-SaaS V7.0 MANIFEST','',f'Total files (excluding MANIFEST): {len(files)}','','## Files','']
for p in files:
    h=hashlib.sha256(p.read_bytes()).hexdigest()[:16]
    lines.append(f'- `{p.relative_to(root)}` — sha256:{h}')
manifest.write_text('\n'.join(lines)+'\n',encoding='utf-8')
print(f'Wrote {manifest} with {len(files)} files')
