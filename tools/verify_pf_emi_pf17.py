#!/usr/bin/env python3
"""Offline source-contract checks for pf.17. NOT a Minecraft build or gameplay test."""
from pathlib import Path
import json, re
root = Path(__file__).resolve().parents[1]
plugin = (root/'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java').read_text()
loot = (root/'xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintLootRecipe.java').read_text()
grid = (root/'xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintGridRecipe.java').read_text()
props = (root/'gradle.properties').read_text()
changelog = (root/'CHANGELOG.md').read_text()
readme = (root/'README.md').read_text()
source_doc = (root/'docs/PF17_PROJECTFLINT_0.31.55_PARITY.md').read_text()
lang = json.loads((root/'xplat/src/main/resources/assets/emi/lang/en_us.json').read_text())
other_lang = json.loads((root/'xplat/src/main/resources/assets/pf_emi/lang/en_us.json').read_text())
basin = plugin.split('private static void registerBasinRoutes',1)[1].split('private static void registerFireclayForgeRoutes',1)[0]
drops = plugin.split('private static void registerBlockDropRecipes',1)[1].split('private static void registerMobLootRecipes',1)[0]
hardwood = plugin.split('private static void registerHardwoodCrafting',1)[1].split('private static void registerBlockDropRecipes',1)[0]
personal = plugin.split('private static void registerPersonalCrafting',1)[1].split('private static void registerHardwoodCrafting',1)[0]
checks={
 'mod release property': 'mod_version=1.1.23-pf.17' in props,
 'plugin release property': 'PF_EMI_PATCH_LEVEL = "pf.17"' in plugin,
 'changelog pins GitHub commit': '54184ab574bc1999ad616d91518d4681365fca96' in changelog,
 'README pins ProjectFlint head': '54184ab574bc1999ad616d91518d4681365fca96' in readme,
 'build uses new output name': 'PF-EMI-1.21.11-pf.17.jar' in (root/'build.bat').read_text(),
 'new apply/builder bat': 'call build.bat' in (root/'APPLY_PF_EMI_PF_17.bat').read_text(),
 'no recipe catchall': 'recent_world_processing' not in plugin,
 'basin category': 'category(registry, "basin", "basin")' in basin,
 'basin ten distinct input processes': len(re.findall(r'basinMaterial\(registry, basin, "(?:raw_iron|raw_copper|andesite|granite|stone|diorite|stone_dust|gravel|sand|slag)"',basin))==10,
 'iron guaranteed and rolls': all(x in basin for x in ('Guaranteed: 1 Iron Dust','Iron Dust, 75%','Slag, 50%')),
 'copper all three dust rolls': all(x in basin for x in ('Copper Dust, 83%','Copper Dust, 24%','Nickel Dust, 10%','Silver Dust, 2%')),
 'fertilizer 50/50': basin.count('1 Fertilizer (50%) or 2 Fertilizer (50%)')==2,
 'stone dust 50/50': basin.count('1 Stone Dust (50%) or 2 Stone Dust (50%)')==2,
 'slag optional': '50% chance of 1 Slag; otherwise no output' in basin,
 'gravel optional': 'Quartz Dust, 20%' in basin and 'Iron Dust, 1%' in basin,
 'sand optional': 'Mica Dust, 5%' in basin and 'Gold Dust, 1%' in basin,
 'slag independent extras': all(x in basin for x in ('Iron Dust, 33%', 'Copper Dust, 33%', 'Silicon Dust, 33%')),
 'one food point': 'Cost: 1 food point per item' in basin,
 'jump-processing tooltip': 'jump on the Basin' in basin,
 'potential bonus slots use source-defined chances': 'basin/material/' in basin and 'loot(' in basin,
 'hazards registered separately': 'registerUndergroundHazards(registry)' in plugin,
 'Y thresholds in displays': 'Below Y=0' in plugin and 'Below Y=-40' in plugin,
 'light exemption': 'block light 13+' in plugin,
 'stacks 60sec four levels': 'Stacks every 60 seconds, up to level IV' in plugin,
 'claustrophobia debuff': 'Movement speed -7%; attack damage -1' in plugin,
 'hazards category translated': lang['emi.category.projectflint.environmental_hazards']=='Underground Hazards and Safety',
 'hazards translation mirrored': other_lang['emi.category.projectflint.environmental_hazards']=='Underground Hazards and Safety',
 'no arrow on no-output info pages': 'if (!outputs.isEmpty()) widgets.addTexture(EmiTexture.EMPTY_ARROW' in loot,
 'furnace exclusive cobblestone->stone': '"stone_from_cobblestone", mc("cobblestone"), mc("stone"), 300, 1.50f' in plugin,
 'blast lacks special cobble': 'stone_from_cobblestone' not in plugin.split('if (blast != null) {',1)[1].split('}',1)[0],
 'stone tool timings': '13.5 seconds with Wooden/Hardwood Pickaxe' in drops and '12 seconds with Hardwood Hammer' in drops,
 'coal hammer 2': 'mc("coal", 2)' in drops,
 'coal only normal': 'for (String ore : List.of("coal_ore", "deepslate_coal_ore"))' not in drops,
 'deepslate totally locked': '"drops/locked_deepslate"' in drops and 'deepslate_coal_ore' in drops,
 'raw iron exactly one': 'Exactly 1 Raw Iron' in drops,
 'raw copper exactly one': 'Exactly 1 Raw Copper' in drops,
 'decoration hand recovery': all('"'+x+'"' in drops for x in ('fat_burning_torch','flint_torch','nest','axle')),
 'Nest egg inventory note': 'Stored Nest eggs are ejected' in drops,
 'vanilla torch pickup': all('"'+x+'"' in drops for x in ('torch','soul_torch','redstone_torch')),
 'Axle placement category': '"placement/axle"' in plugin and 'do not transmit power' in plugin,
 'Hardwood actual source type': 'pfId("hardwood_crafting")' in hardwood and 'getAllRecipesOfType(registry.getRecipeManager(), hardwoodType)' in hardwood,
 'Hardwood source JSON fallback': 'rootPath.resolve("data/projectflint/recipe")' in hardwood,
 'Hardwood shaped/shapeless/durable types': all(x in hardwood for x in ('projectflint:hardwood_shaped','projectflint:hardwood_shapeless','projectflint:hardwood_durable')),
 'Hardwood source parsing no copied outdated recipes': 'JsonParser.parseReader(reader)' in hardwood and 'grid(registry, hardwood, "hardwood_axe"' not in hardwood,
 'nine Hinged Door 2x2 variants': 'kind + "_door_hinged"' in personal and len(re.findall(r'"oak"|"spruce"|"birch"|"jungle"|"acacia"|"dark_oak"|"mangrove"|"cherry"|"pale_oak"',plugin.split('WOOD_VARIANTS = List.of(',1)[1].split(');',1)[0]))==9,
 'doors remapped without vanilla duplicate': 'ids.add(wood + "_door_hinged")' in plugin,
 'no retired wooden tool 2x2 displays': all(f'grid(registry, personal, "{x}"' not in personal for x in ('wooden_axe','wooden_pickaxe','wooden_shovel')),
 'vest exclusive to Wooden Grinder': 'if (id.getPath().equals("tanned_utility_vest")) continue;' in plugin,
 'Hardwood 3 second timer preserved': 'Craft: 3s (60t)' in grid,
 'reference documentation exists': all(x in source_doc for x in ('0.31.53-b','0.31.54-b','0.31.55-b')),
 'source docs include unchecked live tests': '[ ] **Full Gradle Fabric compilation**' in source_doc,
 'source-parity probabilities mentioned': 'All bonus percentages represent **independent Bernoulli trials**' in source_doc,
 'versioned changelog': '## 1.1.23-pf.17' in changelog,
}
for name,value in checks.items(): print(('PASS' if value else 'FAIL')+' '+name)
failed=[k for k,v in checks.items() if not v]
print(f'\n{len(checks)-len(failed)}/{len(checks)} static checks passed')
if failed: raise SystemExit(1)
