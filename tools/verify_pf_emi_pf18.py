#!/usr/bin/env python3
"""Offline PF-EMI pf.18 static checks. Does not replace compilation or gameplay tests."""
from pathlib import Path
import re,sys
ROOT = Path(__file__).resolve().parents[1]
PLUGIN = (ROOT/'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java').read_text()
BASIN = (ROOT/'xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintBasinRecipe.java').read_text()
LOOT = (ROOT/'xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintLootRecipe.java').read_text()
GRID = (ROOT/'xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintGridRecipe.java').read_text()
FAIL=[];OK=0

def check(ok, message):
    global OK
    OK+=1
    if not ok: FAIL.append(message)

check('PF_EMI_PATCH_LEVEL = "pf.18"' in PLUGIN,'plugin version')
check('mod_version=1.1.23-pf.18' in (ROOT/'gradle.properties').read_text(),'Gradle version')
check('## 1.1.23-pf.18' in (ROOT/'CHANGELOG.md').read_text(),'changelog header')
check('APPLY_PF_EMI_PF_18.bat' in [p.name for p in ROOT.glob('*.bat')],'apply script')
check('registerBasinRoutes(registry)' in PLUGIN,'basin registry call')
check('new ProjectFlintBasinRecipe(' in PLUGIN,'compact basin class used')
check('private static void basinMaterial(' not in PLUGIN,'obsolete basin long-form route removed')
check('1 food per item' not in PLUGIN,'old Basin cost language removed')
check('stomps + " stomps - 1 food each"' in BASIN,'correct cost and stomp line')
check('Each stomp uses 1 food point' in BASIN,'per-stomp explanation')
check('getCatalysts() { return List.of(basin); }' in BASIN,'basin is workstation catalyst')
check('result.note()' in BASIN,'per-output chance tooltips')
check('results.size() > 3 ? 65 : 47' in BASIN,'two-row output layout')
check('EmiIngredient.of(TagKey.of(RegistryKeys.ITEM, id("minecraft", "planks")))' in PLUGIN,'all planks tag')
check('"stripped_oak_log", "stripped_oak_wood", "stripped_birch_log", "stripped_birch_wood"' in PLUGIN,'stripped wood selection')

stomps={
 'wood_dust':2,'tannin':3,'raw_iron':3,'raw_copper':3,
 'andesite':3,'granite':3,'diorite':3,'stone_dust':2,
 'gravel':2,'sand':2,'clay':3,'rare_earth_dust':3,'slag':3
}
for name,amount in stomps.items():
 pattern=rf'basin\(registry, basin, "{name}".*?,\s*{amount},\s*(?:List\.of\()?'
 check(re.search(pattern,PLUGIN,re.S) is not None, f'{name} stomp cost {amount}')
check('List.of("stone", "cobblestone")' in PLUGIN,'Stone and Cobblestone together')
check('basin(registry, basin, stone, mc(stone), 3' in PLUGIN,'Stone and Cobblestone 3 stomps')

# Exact source independent output chances from ProjectFlint 0.31.57.
for name, rates in {
 'raw_iron':['75%','50%','3%'],
 'raw_copper':['83%','24%','10%','2%','5%'],
 'andesite':['50%','50%','33%'],
 'granite':['50%','50%','33%'],
 'diorite':['50%','50%'],
 'stone_dust':['50%'],
 'gravel':['20%','1%'],
 'sand':['15%','5%','1%'],
 'clay':['5%','3.33%','0.33%','2%'],
 'rare_earth_dust':['22.3%','15%','4.8%','0.85%'],
 'slag':['33%','33%','33%']
}.items():
 start=PLUGIN.find('basin(registry, basin, "'+name+'"')
 end=PLUGIN.find('basin(registry, basin,',start+15)
 if end < 0: end=PLUGIN.find('\n\t}', start)
 body=PLUGIN[start:end]
 present=re.findall(r'[0-9]+(?:\.[0-9]+)?%',body)
 check(sorted(present)==sorted(rates),f'{name} odds mismatch: {present} != {rates}')
for item in ['stone_dust','stone','cobblestone']:
 check('"'+item+'"' in PLUGIN,'Stone-family recipe '+item)
for id in ['mbssig_dust','pure_mica_insulated_glass','hardwood_alloy']:
 check('pf("'+id+'")' in PLUGIN,'forge item '+id)
check('"forge/mica_insulated_glass"' in PLUGIN,'new forge recipe')
check('20 * 60 * 5' in PLUGIN,'5-minute forge cook')
check('"hardwood_crafting_table",' in PLUGIN,'bootstrap hard-crafting-table recipe')
check('List.of(pf("hardwood_alloy"), pf("hardwood_alloy"), mc("cobblestone"), pf("bound_wood_processor"))' in PLUGIN,'exact bootstrap inputs')
check('pf("leather_strap", 3)' in PLUGIN,'Leather Strap yield 3')
check('pf("repair_kit", 2)' in PLUGIN,'Repair Kit yield 2')
check('registerBundledHardwoodRecipes' in PLUGIN,'custom type JSON fallback preserved')
check('EmiAgnos.getAllRecipesOfType(registry.getRecipeManager(), hardwoodType)' in PLUGIN,'custom type lookup preserved')
check('reusableToolIndices' in GRID,'multiple tool catalysts')
check('projectflint:flint_file' in GRID and 'projectflint:bone_knife' in GRID,'Knife/File remainders')
check('getDisplayHeight()' in LOOT and '19 * Math.max(0, (outputs.size() - 1) / 6)' in LOOT,'multi-row mob output rendering')
check('Math.min(lines.size(), 2)' in LOOT,'bounded notes and headings')
check('subjectSlot.appendTooltip' in LOOT,'details moved to hover')
check('"Crafting time: 3 seconds"' in GRID and '60t' not in GRID,'player-facing crafting time')
check('0.31.57-b' in (ROOT/'docs/PF18_PROJECTFLINT_0.31.57_PARITY.md').read_text(),'upstream comparison documented')

for item in FAIL:print('FAIL:',item)
print(f'PF18 SOURCE CHECKS: {OK-len(FAIL)}/{OK} passed')
if FAIL:sys.exit(1)
