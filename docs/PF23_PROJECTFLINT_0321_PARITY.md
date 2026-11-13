# PF-EMI pf.23 — ProjectFlint 0.32.1 Source Parity

Source: `bostonstuddard/ProjectFlint` at commit `796d129dadbab83dc46dab1b5652b2bafe92eb3d`. Only implemented gameplay changes are represented; proposals for issues #17 and #25 are excluded.

| Gameplay change | EMI category/route | Display |
|---|---|---|
| Woven Chest Frame | Wooden Grinder | 2 Leather + 4 Woven Wood → frame |
| Wattled-Daub Windows | Wooden Grinder | 4 Wattled Daub Bricks + Advanced Kiln Glass → 3 Windows |
| Wattle alternative | Wooden Grinder | 3 Sugar Cane + 2 Branches → 4 Wattle |
| Water Bearing Pot | Wooden Grinder | 6 Woven Wood + Clay Pot → Pot |
| Grinded Wood Slabs | Wooden Grinder | 3 Sanded Wood Slabs + reusable Flint File → 2 Grinded Slabs |
| Woven Wood alternative | Wooden Grinder | 3 Grinded Wood Slabs + 3 Woven Fans → 2 Woven Wood |
| Unfired Clay Bucket (Bone Knife) | Hardwood Crafting Table (3×3) | 3 Clay Balls + reusable Bone Knife → 1; removed from Wooden Grinder |
| Unfired Clay Bucket (Frame) | Wooden Grinder | Retained: 3 Clay Balls + Woven Chest Frame → 1 |
| Hardwood production | Hardwood Crafting Table (3×3) | 5 Wood Dust + Clay Water Bucket + 3 Tannin → **3 Hardwood**; bucket remainder from ProjectFlint recipe |
| Wooden Cabinet | Hardwood Crafting Table (3×3) | Existing grid now produces 4 Cabinets |
| Mortar Kiln Base | Clay Grinder, Wooden Grinder conversion | 24 Mortar Bricks → 1 |
| Wooden slab → stripped log | Clay Grinder, Wooden Grinder conversion | Removed all nine reverse log-variant routes and bamboo reverse |
| Standard mold casting | Advanced Kiln | 8 Burnt Branches + 6 Flint + 4 Fired Clay + mold → corresponding output; special cycles unchanged |
| Bookshelf Book | Bookshelf | 8 Sugarcane Rolls + 1 Leather → 1 Book |
| Mutton Pie | Primitive Stove | 4 Cooked Mutton + 2 Sugar → 3 Pies |
| Sweet Berry Jam | Primitive Stove | 8 Sweet Berries + Sugar + 2 Wooden Cups → 4 Jam |
| Strawberry Jam | Primitive Stove | 12 Strawberries + Sugar + 2 Wooden Cups → 4 Jam |
| Lumber Processor | Lumber Processing | 90-tick chopping + 200-tick washing (= 14.5s at 20 TPS); existing output modes unchanged |
| Fill empty Water Pot | World Interactions | Clay Water Bucket + empty placed Water Pot → full Pot; returns Clay Bucket |
| Placed String retrieval | World Interactions | Recover placed String by hand |
| Flint Plant Cutters leaves | Block Drops | 5 Branches plus existing sapling/Stick/apple chance drops |
| Special Project Flint Brush | World Interactions | 512-durability Brush shears 4–5 Wool (Bone Brush remains 3–4) |
| Cow / Bear leather | Mob Drops | Approved adult-cow Raw Leather guarantee and 2–3 extra Raw Leather for adult bears require runtime drop-table verification |

## Not recipes

- Double-height flower harvest fixes, Sugar Cane planting fixes, tool-model/ground-texture fixes, safe denied-ore destruction and adjusted 35-tick brush progress require no separate crafting recipe.
- Issue #17 animal-byproduct craft ideas and issue #25 cooking rebalance are **proposals only**, not gameplay routes.
- The Wooden Chest upgrade (frame on placed Woven Chest) and v0.31.69 Dynamite/Mud information remain from pf.22.

## Source-owned recipes

The Hardwood 3×3 entries are read from ProjectFlint's synchronized `hardwood_crafting` recipe type, or installed mod JSON when synchronization is absent. PF-EMI must not invent recipes or duplicate them in the normal Crafting category.
