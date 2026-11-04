#!/usr/bin/env python3
"""Drift check: favorites-api DTO copies vs gisorch originals.

Compares field sets (name + type) and enum constants. Exits non-zero on drift.
Ignores annotations, javadoc, formatting: only the JSON-relevant shape matters.
"""
import re
import sys
from pathlib import Path

ORCH = Path("/java/czh/czh-favourites/references/czh-gisorch/czh-gis-api/src/main/java/cz/bsl/gis/api/drawgames")
API = Path("/java/czh/czh-favourites/czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api")

FILES = {"WagerDTO": "WagerDto", "BoardDTO": "BoardDto", "StakeDTO": "StakeDto",
         "TaxDTO": "TaxDto", "TextDTO": "TextDto", "AddressDTO": "AddressDto",
         "IdDocumentDTO": "IdDocumentDto", "PrizeDTO": "PrizeDto",
         "PromotionDTO": "PromotionDto", "TicketSecurityDTO": "TicketSecurityDto",
         "ErrorDTO": "ErrorDto", "FieldErrorDTO": "FieldErrorDto",
         "WagerType": "WagerType", "PrizeStatus": "PrizeStatus", "PrizeType": "PrizeType"}

RENAME = {k: v for k, v in FILES.items() if k.endswith("DTO")}


def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    return src


def shape(path: Path) -> dict:
    src = strip_comments(path.read_text(encoding="utf-8"))
    src = "\n".join(line.expandtabs(4).rstrip() for line in src.splitlines())
    fields = {}
    for m in re.finditer(
            r"^\s*(?:private|protected)\s+(?:static\s+)?(?:final\s+)?"
            r"([\w.<>\[\],\s]+?)\s+(\w+)\s*(?:=[^;]*)?;", src, re.M):
        typ = re.sub(r"\s+", " ", m.group(1)).strip()
        fields[m.group(2)] = typ
    consts = []
    if re.search(r"\benum\s+\w+", src.split("{", 1)[0]):
        body = src.split("{", 1)[1]
        seg = re.split(r"[;}]", body, 1)[0]        # constant list (ends at ; or })
        consts = re.findall(r"\b([A-Z][A-Z0-9_]*)\b", seg)
    return {"fields": fields, "consts": consts}


def normalize(s: str) -> str:
    for old, new in RENAME.items():
        s = re.sub(rf"\b{old}\b", new, s)
    return s


def main() -> int:
    drift = 0
    for orig, copy in FILES.items():
        o = shape(ORCH / f"{orig}.java")
        c = shape(API / f"{copy}.java")
        o_fields = {k: normalize(v) for k, v in o["fields"].items()}
        c_fields = c["fields"]
        problems = []
        if o_fields != c_fields:
            for k in sorted(set(o_fields) | set(c_fields)):
                if o_fields.get(k) != c_fields.get(k):
                    problems.append(f"field {k}: gisorch={o_fields.get(k)} favorites={c_fields.get(k)}")
        if sorted(o["consts"]) != sorted(c["consts"]):
            problems.append(f"constants: gisorch={sorted(o['consts'])} favorites={sorted(c['consts'])}")
        if problems:
            drift += 1
            print(f"DRIFT {copy}:")
            for p in problems:
                print(f"  {p}")
        else:
            print(f"ok    {copy} ({len(c_fields)} fields, {len(c['consts'])} constants)")
    return 1 if drift else 0


if __name__ == "__main__":
    sys.exit(main())
