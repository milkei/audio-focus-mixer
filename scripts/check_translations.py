"""Check resource-key and formatting-placeholder parity between English and Korean."""
from collections import Counter
from pathlib import Path
import re
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/res"
EN = ROOT / "values/strings.xml"
KO = ROOT / "values-ko/strings.xml"
TOKEN = re.compile(r"%(?:\d+\$)?[sd]")


def parse(path: Path) -> dict[str, str]:
    root = ElementTree.parse(path).getroot()
    values = {}
    for child in root.findall("string"):
        key = child.attrib["name"]
        if key in values:
            raise ValueError(f"Duplicate key {key} in {path}")
        values[key] = "".join(child.itertext())
    return values


def main() -> None:
    en, ko = parse(EN), parse(KO)
    assert en and ko, "String resources must not be empty"
    assert en.keys() == ko.keys(), (
        f"Translation key mismatch: missing in Korean={sorted(en.keys() - ko.keys())}; "
        f"missing in English={sorted(ko.keys() - en.keys())}"
    )
    for key in en:
        assert en[key].strip() and ko[key].strip(), f"Empty value for {key}"
        a, b = Counter(TOKEN.findall(en[key])), Counter(TOKEN.findall(ko[key]))
        assert a == b, f"Placeholder mismatch for {key}: {a} vs {b}"
    print(f"OK: {len(en)} English and Korean strings share the same keys/placeholders")


if __name__ == "__main__":
    main()
