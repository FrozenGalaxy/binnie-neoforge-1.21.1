#!/usr/bin/env python3
"""Generate model/blockstate JSON for the source-registered Extra Trees liquids."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
catalog = (ROOT / "src/main/resources/assets/binnietrees/lang/en_us.json")
lang = json.loads(catalog.read_text(encoding="utf-8"))

import re
source = (ROOT / "src/main/java/binnie/trees/beverage/LegacyBeverageCatalog.java").read_text(encoding="utf-8")
entries = re.findall(r'put\(values, "([^"]+)", "([^"]+)", "([^"]+)"', source)
for category, value, legacy_id in entries:
    if category not in {"juice", "alcohol", "spirit", "liqueur"}: continue
    fluid_id = legacy_id.lower()
    blockstate = ROOT / f"src/main/resources/assets/binnietrees/blockstates/{fluid_id}.json"
    blockmodel = ROOT / f"src/main/resources/assets/binnietrees/models/block/{fluid_id}.json"
    bucketmodel = ROOT / f"src/main/resources/assets/binnietrees/models/item/{fluid_id}_bucket.json"
    for path in (blockstate, blockmodel, bucketmodel):
        path.parent.mkdir(parents=True, exist_ok=True)
    blockstate.write_text(json.dumps({"variants": {"": {"model": f"binnietrees:block/{fluid_id}"}}}, separators=(",", ":")) + "\n", encoding="utf-8")
    blockmodel.write_text(json.dumps({"textures": {"particle": "binnietrees:block/fluid/beverage"}}, separators=(",", ":")) + "\n", encoding="utf-8")
    bucketmodel.write_text(json.dumps({"parent": "neoforge:item/bucket", "fluid": f"binnietrees:{fluid_id}", "loader": "neoforge:fluid_container"}, indent=2) + "\n", encoding="utf-8")
    fluid_key = f"item.binnietrees.{category}.{value}"
    lang[f"item.binnietrees.{fluid_id}_bucket"] = lang.get(fluid_key, value.replace('_', ' ').title()) + " Bucket"

catalog.write_text(json.dumps(dict(sorted(lang.items())), indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
