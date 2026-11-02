#!/usr/bin/env python3
"""Static pf.15 source-contract assertions only. No Minecraft runtime claims."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
plugin = (root / 'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java').read_text()
changelog = (root / 'CHANGELOG.md').read_text()
props = (root / 'gradle.properties').read_text()
checks = {
    'version': 'mod_version=1.1.23-pf.15' in props,
    'commit pinned': '37c5f2af2a9939ac71c9d04664954caf10beeb7c' in changelog,
    'runtime hardwood type': 'pfId("hardwood_crafting")' in plugin,
    'hardwood type query': 'getAllRecipesOfType(registry.getRecipeManager(), hardwoodType)' in plugin,
    'recipe entry ids': 'entry.id().getValue()' in plugin,
    'unwrap custom wrapper': 'unwrapHardwoodCrafting' in plugin and 'getDeclaredField("delegate")' in plugin,
    'shaped grid geometry': 'shaped.getWidth()' in plugin and 'shaped.getHeight()' in plugin,
    'shapeless alternatives': 'getIngredientPlacement().getIngredients()' in plugin,
    'real recipe outputs': 'EmiPort.getOutput(wrapped)' in plugin,
    'bundle fallback': 'registerBundledHardwoodRecipes' in plugin,
    'reads actual mod data': 'rootPath.resolve("data/projectflint/recipe")' in plugin,
    'custom hardwood serializers': all(x in plugin for x in ('projectflint:hardwood_shaped', 'projectflint:hardwood_shapeless','projectflint:hardwood_durable')),
    'reads real source JSON': 'JsonParser.parseReader(reader)' in plugin,
    'supports tag ingredients': 'TagKey.of(RegistryKeys.ITEM' in plugin,
    'durable tool ingredient': 'json.get("tool")' in plugin,
    'bucket remainder': 'option.setRemainder(pf("clay_bucket"))' in plugin,
    'unfired mold remainder': 'option.setRemainder(pf("unfired_brick_mold"))' in plugin,
    'no hardcoded 6 tools': 'grid(registry, hardwood, "hardwood_axe"' not in plugin,
    'no station table in personal': 'grid(registry, personal, "hardwood_crafting_table"' not in plugin,
    'no retired wooden tools in personal': all(f'grid(registry, personal, "{x}"' not in plugin for x in ('wooden_axe','wooden_pickaxe','wooden_shovel')),
    'no door hinges in personal': '"_door_hinged"' not in plugin[plugin.index('private static void registerPersonalCrafting'):plugin.index('private static void registerHardwoodCrafting')],
    'proper vanilla wooden pickaxe': 'mc("wooden_pickaxe")' in plugin,
    'hardwood ore drops': all(x in plugin for x in ('ore/iron_hardwood_hammer','ore/copper_hardwood_hammer','mc("coal", 2)')),
    '13.5 second ore claims': '13.5 seconds' in plugin,
    '45 second ore claims': '45 seconds' in plugin,
    'tool timer display preserved': 'Craft: 3s (60t)' in (root/'xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintGridRecipe.java').read_text(),
    'build name': 'pf.15' in (root/'build.bat').read_text(),
    'apply script': (root/'APPLY_PF_EMI_PF_15.bat').is_file(),
    'category english': 'Hardwood Crafting Table (3x3)' in (root/'xplat/src/main/resources/assets/emi/lang/en_us.json').read_text(),
    'durable serializer identification': 'pfId("hardwood_durable").equals(serializer)' in plugin,
    'durable grid catalysts': 'List.of(cells.get(reusableToolIndex))' in (root/'xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintGridRecipe.java').read_text(),
    'fallback durable identification': 'type.equals("projectflint:hardwood_durable")' in plugin,
    'current docs': 'v0.31.52-b' in (root/'README.md').read_text(),
    'balanced plugin braces': plugin.count('{') == plugin.count('}'),
}
for name, passed in checks.items():
    print(f"{'PASS' if passed else 'FAIL'} - {name}")
failures = [k for k, v in checks.items() if not v]
print(f"\n{len(checks)-len(failures)}/{len(checks)} static assertions passed")
if failures:
    raise SystemExit(1)
