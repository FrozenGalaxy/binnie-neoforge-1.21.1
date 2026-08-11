#!/usr/bin/env python3
"""Generate the state-exact 1.12 gardening conversion recipes."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/data/binniebotany/recipe/gardening"
OUT.mkdir(parents=True, exist_ok=True)

families = ("soil", "loam", "flowerbed")
acids = ("acid", "neutral", "alkaline")
moistures = ("dry", "normal", "damp")

def state(family, acid, moisture, weed):
    return {
        "id": f"binniebotany:{family}",
        "count": 4,
        "components": {"minecraft:block_state": {
            "acidity": acid, "moisture": moisture,
            "fertility": str(("soil", "loam", "flowerbed").index(family) + 1),
            "weedkilled": str(weed).lower()
        }}
    }

def ingredient(family, acid, moisture, weed):
    component = state(family, acid, moisture, weed)["components"]
    return {"type": "neoforge:components", "items": f"binniebotany:{family}", "components": component}

def recipe(name, family, acid, moisture, weed, reagent, out_family, out_acid, out_weed):
    source = ingredient(family, acid, moisture, weed)
    data = {"type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [source, source, source, source, {"item": reagent}],
            "result": state(out_family, out_acid, moisture, out_weed)}
    (OUT / f"{name}.json").write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

for family in families:
    for acid_i, acid in enumerate(acids):
        for moisture in moistures:
            recipe(f"weedkiller_{family}_{acid}_{moisture}", family, acid, moisture, False,
                   "binniebotany:weedkiller", family, acid, True)
            for weed in (False, True):
                suffix = "treated" if weed else "normal"
                if acid_i > 0:
                    recipe(f"mulch_{family}_{acid}_{moisture}_{suffix}", family, acid, moisture, weed,
                           "forestry:mulch", family, acids[acid_i - 1], weed)
                if acid_i < 2:
                    for reagent in ("forestry:ash", "forestry:wood_pulp"):
                        short = reagent.split(":", 1)[1]
                        recipe(f"{short}_{family}_{acid}_{moisture}_{suffix}", family, acid, moisture, weed,
                               reagent, family, acids[acid_i + 1], weed)
                if family != "flowerbed":
                    next_family = families[families.index(family) + 1]
                    recipe(f"fertilizer_{family}_{acid}_{moisture}_{suffix}", family, acid, moisture, weed,
                           "forestry:fertilizer", next_family, acid, weed)
