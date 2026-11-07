#!/usr/bin/env python3
"""Static source-contract tests; do NOT interpret as Java compilation or runtime."""
from pathlib import Path
import json,re,sys
root=Path(__file__).resolve().parents[1]
s=(root/'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java').read_text()
props=(root/'gradle.properties').read_text()
checks=[]
def check(label,test):
    checks.append((label,bool(test)))
check('pf20 version', 'mod_version=1.1.23-pf.20' in props and 'PF_EMI_PATCH_LEVEL = "pf.20"' in s)
check('Minecraft 1.21.11', 'minecraft_version=1.21.11' in props)
check('retired stripped-log craft filtered', 'primitive_wooden_sheet_from_stripped_log' in s and 'id.getPath().equals("primitive_wooden_sheet_from_stripped_log")' in s)
check('five early personal tools', all('"'+n+'", 2, true' in s for n in ['wooden_pickaxe','wooden_axe','wooden_shovel','wooden_sword_personal','wooden_hoe_personal']))
check('leather counts', 'pf("leather_strap", 2), true' in s and 'pf("repair_kit"));' in s)
check('mud/grinder costs', 'pf("mud_pile", 8)' in s and 'pf("loose_mud_bricks", 2), pf("wooden_screw", 2)' in s)
check('wooden beam sheet x4', '"wood/wooden_sheet"' in s and 'pf("wooden_sheet", 4)' in s)
check('source-accurate coal/charcoal washes', '"coal", mc("coal"), 3' in s and '"charcoal", mc("charcoal"), 2' in s)
check('coal independent 43/23 rolls', '"Stone Dust: 43%"' in s and '"+1 Stone Dust: 23%"' in s)
check('carbon/sulfur wash', '"Carbon Dust: 2.4%"' in s and '"Sulfur Dust: 45%"' in s)
check('guaranteed fertilizer and bonus', all(x in s for x in ['"1 guaranteed"','"+1 Fertilizer: 50%"']))
check('fuse furnace 200 seconds', '"fine_fuse_powder", pf("fuse_ready_dust"), pf("fine_fuse_powder"), 4000, 1.0f' in s)
check('forge carbon 400 seconds', '"forge/pure_carbon_dust"' in s and 'List.of(pf("pure_carbon_dust")), 400 * 20' in s)
check('separate dynamite category', '"dynamite_blasting"' in s and '"emi.category.projectflint.dynamite_blasting"' in (root/'xplat/src/main/resources/assets/pf_emi/lang/en_us.json').read_text())
check('eight blast targets', all('"'+t+'", mc("'+t+'")' in s for t in ['stone','cobblestone','andesite','granite','diorite','iron_ore','copper_ore','coal_ore']))
check('blasted outputs independent 1.5 rolls', 'roll.normalChance() * 1.5' in s and 'Math.floor(boosted)' in s)
check('hammer converts natural stone', '"stone/crude_hammer_processing"' in s and 'pf("rock", 4)' in s)
check('durable wrapper fallback only for unresolved', 'registerBundledHardwoodRecipes(registry, hardwood, unresolved)' in s and 'onlyIds != null && !onlyIds.contains(recipeId)' in s)
check('hardwood live recipe type', 'pfId("hardwood_crafting")' in s and 'EmiAgnos.getAllRecipesOfType' in s)
check('no catch-all newer world category', 'recent_world_processing' not in s)
check('old 16-mud Grinder route gone', 'pf("mud_pile", 16)), pf("loose_mud_bricks")' not in s)
check('personal Pmi recipe retired', 'id.getPath().equals("personal_pmi_sheets")' in s)
check('recipe trees retained', 'supportsRecipeTree() { return true; }' in (root/'xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintGridRecipe.java').read_text())
check('transfer handlers retained', 'registerStationTransferHandlers(registry)' in s)
check('language valid JSON', bool(json.loads((root/'xplat/src/main/resources/assets/pf_emi/lang/en_us.json').read_text())))
check('no generated output in patch manifest', 'no nested parent folder' in (root/'PATCH_MANIFEST.md').read_text().lower())
for name,ok in checks: print(('PASS' if ok else 'FAIL'),name)
print(f"{sum(ok for _,ok in checks)}/{len(checks)} static checks passed")
sys.exit(0 if all(ok for _,ok in checks) else 1)
