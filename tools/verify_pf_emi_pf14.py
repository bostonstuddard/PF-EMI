#!/usr/bin/env python3
"""Offline PF-EMI pf.14 structural checks. Not Minecraft gameplay/runtime tests."""
from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
java=root/'xplat/src/main/java/dev/emi/emi'
plugin=(java/'ProjectFlintPlugin.java').read_text()
loot=(java/'recipe/ProjectFlintLootRecipe.java').read_text()
grid=(java/'recipe/ProjectFlintGridRecipe.java').read_text()
structure=(java/'recipe/ProjectFlintMultiblockRecipe.java').read_text()
viewport=(java/'widget/ProjectFlintMultiblockWidget.java').read_text()
checks={
 'Correct PF-EMI version': 'mod_version=1.1.23-pf.14' in (root/'gradle.properties').read_text(),
 'Correct build version': 'pf.14' in (root/'build.bat').read_text(),
 'Upstream version pinned': '169797e5440474457fd3410456bf43401214bf3c' in (root/'CHANGELOG.md').read_text(),
 'Visually bounded notes': 'for (String line : wrap(note))' in loot and 'getDisplayHeight()' in loot,
 'Light theme readable dark text': '0xFF303030' in loot and '0xFF242424' in loot,
 'No shapeless badge above output': 'widgets.addTexture(EmiTexture.SHAPELESS' not in grid,
 'Visible 3-second craft caption': 'Craft: 3s (60t)' in grid,
 'Multiblock actual item slots': structure.count('widgets.addSlot(') >= 2,
 'Materials show exact accepted variants': 'materialIngredient(material)' in structure and 'material.stack().candidates()' in structure,
 'Material rails not drawn as dead icons': 'renderMaterialRail(context, draw, delta)' not in viewport,
 'Material slot hover not intercepted': 'railTooltip(mouseX, mouseY)' not in viewport,
 'Hardwood recipes dynamically categorized': 'registerOtherProjectFlintCrafting(registry, hardwood)' in plugin and 'AUTO_REMAPPED_RECIPES.add' in plugin,
 'Native shaped recipes synced': 'new EmiShapedRecipe(shaped)' in plugin,
 'Native shapeless recipes synced': 'new EmiShapelessRecipe(sh)' in plugin,
 'Native crafting duplication filtered': 'AUTO_REMAPPED_RECIPES.contains(id.getPath())' in plugin,
 'Vest crafting duplicate excluded': 'pfId("tanned_utility_vest").equals(out.getId())' in plugin,
 'Fat torch recipe excludes Fat': 'List.of(mc("stick"), mc("string"), mc("charcoal")), pf("fat_burning_torch")' in plugin,
 'Hardwood Table recipe uses Bound Wood Processor': 'mc("cobblestone"), pf("bound_wood_processor")' in plugin,
 'Hardwood Axe valid in plank craft': 'any(1, mc("wooden_axe"), pf("hardwood_axe"))' in plugin,
 'Cow milking has Clay Milk Bucket output': '"mob/cow_milking"' in plugin and 'pf("clay_milk_bucket")' in plugin,
 'Bucket water source interaction': '"bucket/collect_water"' in plugin,
 'No fake recent_world_processing': 'recent_world_processing' not in plugin,
 'Registered Wooden Grinder still exists': '"wooden_grinder"' in plugin,
 'Validation report marks runtime open': '- [ ] Java/Gradle compilation' in (root/'docs/PROJECTFLINT_EMI_AUDIT_CHECKLIST.md').read_text(),
 'Apply script guards version': 'mod_version=1.1.23-pf.14' in (root/'APPLY_PF_EMI_PF_14.bat').read_text()
}
for name,good in checks.items(): print(('PASS' if good else 'FAIL')+' '+name)
print(f'{sum(checks.values())}/{len(checks)} passed')
sys.exit(0 if all(checks.values()) else 1)
