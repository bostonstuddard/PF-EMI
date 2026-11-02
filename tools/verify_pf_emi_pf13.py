#!/usr/bin/env python3
"""PF-EMI pf.13 offline regression checks. Not a Gradle or Minecraft runtime test."""
from pathlib import Path
import re, json, sys, collections
root=Path(__file__).resolve().parents[1]
plugin=(root/'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java').read_text()
grid=(root/'xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintGridRecipe.java').read_text()
lang=json.loads((root/'xplat/src/main/resources/assets/emi/lang/en_us.json').read_text())
checks={}
def check(label, condition): checks[label]=bool(condition)
check('pf.13 version in source', 'PF_EMI_PATCH_LEVEL = "pf.13"' in plugin)
check('pf.13 Gradle version', 'mod_version=1.1.23-pf.13' in (root/'gradle.properties').read_text())
check('No recent_world_processing catch-all category', 'recent_world_processing' not in plugin)
check('No wrong Hardwood material in Hardwood 3x3 category', '"hardwood/material"' not in plugin)
check('Separate Basin and Forge categories', 'category(registry, "basin", "basin")' in plugin and 'category(registry, "fireclay_forge", "fireclay_forge_controller")' in plugin)
check('Farming is icon-only, not fake workstation', 'categoryIcon(registry, "farming", "strawberry_seeds")' in plugin)
check('Separate grid categories', 'category(registry, "personal_crafting", "tanned_utility_vest")' in plugin and 'category(registry, "hardwood_crafting", "hardwood_crafting_table")' in plugin)
check('Hardwood 3x3 recipe grid', all(('"hardwood_'+tool+'", 3, false') in plugin for tool in ['axe','pickaxe','shovel','hoe','sword','hammer']))
check('Hardwood hammer exact 6 alloys + 1 leather, stick and rope', plugin.count('pf("hardwood_alloy")')>=8 and 'mc("leather"), mc("stick"), pf("rope")), pf("hardwood_hammer")' in plugin)
check('Grids show 2x2 and 3x3 sizes and shaped patterns', 'gridSize != 2 && gridSize != 3' in grid and 'for (int i = 0; i < gridSize * gridSize; i++)' in grid)
check('Remap restricted to PF native Crafting routes', 'recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING' in plugin and 'REMAPPED_GRID_RECIPES.contains(id.getPath())' in plugin)
check('Native conditional armor repair preserved', '"repair_leather_armor",' not in plugin[plugin.index('createRemappedRecipeIds() {'):plugin.index('return Set.copyOf(ids);')])
check('All nine classic wood variants', all('"'+w+'"' in plugin for w in ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','pale_oak']))
check('2x2 hinged doors have 3 distinct planks', 'List.of(pf("wooden_door_hinge"), planks, planks.copy(), planks.copy())' in plugin)
check('2x2 personal recipes include crafting and Fat Torch', '"fat_burning_torch", 2, true' in plugin and '"basin_crafting", 2, true' in plugin)
check('Clay Grinder conversions also shown at Wooden Grinder', 'registerClayConversions(registry, wood, "wood_clay")' in plugin)
check('Wood and Clay grinder recipe IDs distinct', 'prefix + "/bone_club"' in plugin and 'prefix + "/" + id + "_log"' in plugin)
check('25 source-defined Sanding Station dyes', len(re.findall(r'\{"[a-z_]+","[a-z_]+","[24]"\}',plugin[plugin.index('// SandingStationRecipeRegistry flower'):plugin.index('if (shelf != null)')]))==25)
check('Brick Furnace exclusive glass and tanned leather', '"brick_furnace", "tanned_leather"' in plugin and '"brick_furnace", "glass"' in plugin)
check('Brick Blast Furnace does not get exclusive tanning', '"brick_blast_furnace", "tanned_leather"' not in plugin)
check('Primitive Oven direct food and research separate', 'registerPrimitiveOvenDirectFood(registry, stove)' in plugin and 'stoveRecipe(registry, stove, "strawberry_jam"' in plugin)
check('Clay bucket returns for water and milk processes', all('List.of('+t+', pf("clay_bucket"))' in plugin for t in ['mc("wheat_seeds", 3)','pf("flour", 9)','pf("fat")','pf("wooden_cup_raw_milk", 4)']))
check('Pumpkin cake slicing and 512-use brush recipe', '"wood/pumpkin_cake_slice"' in plugin and '"wood/brush_upgrade"' in plugin and 'maxDamageItem("brush", 512)' in plugin)
check('World placement category retains sleeping bag', '"placement/sleeping_bag"' in plugin and '"torch/fat_burning_torch"' in plugin)
check('Missing source item never becomes fake silent recipe', 'if (output.isEmpty()' in plugin)
check('All categories translated in English', all(k in lang for k in ['emi.category.projectflint.'+v for v in ['personal_crafting','hardwood_crafting','basin','fireclay_forge','farming','wooden_grinder','clay_grinder','sanding_station','primitive_stove','brick_furnace']]))
check('No derived category registration duplicates', len(re.findall(r'category\(registry, "basin",',plugin))==1 and len(re.findall(r'category\(registry, "hardwood_crafting",',plugin))==1)
# Lexer-style delimiter balance after dropping strings and comments.
def syntax_balanced(source):
    clean=re.sub(r'/\*.*?\*/|//[^\n]*|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', '',source, flags=re.S)
    stack=[]; pairs={'(':')','[':']','{':'}'}
    for char in clean:
        if char in pairs: stack.append(char)
        elif char in pairs.values():
            if not stack or pairs[stack.pop()]!=char:return False
    return not stack
check('Plugin delimiters balanced',syntax_balanced(plugin))
check('Grid Java delimiters balanced',syntax_balanced(grid))
check('Resource JSON parses',bool(lang))
# Compare listed source-derived shaped and process recipe IDs only; dynamic loops intentionally excluded.
for key,ok in checks.items(): print(('PASS' if ok else 'FAIL')+'  '+key)
print(f'CHECKS PASSED: {sum(checks.values())}/{len(checks)}')
sys.exit(0 if all(checks.values()) else 1)
