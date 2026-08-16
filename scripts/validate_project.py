from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
errors = []
for pom in root.rglob("pom.xml"):
    try:
        ET.parse(pom)
    except Exception as exc:
        errors.append(f"{pom}: {exc}")
if errors:
    raise SystemExit("\n".join(errors))
print("PASS: all pom.xml files parse")
