#!/usr/bin/env python3
"""Offline PF-EMI pf.24 source checks. Does not verify Minecraft runtime."""
from pathlib import Path
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java'
s = SRC.read_text(encoding='utf-8')
checks = []

def check(yes, description):
    checks.append(description)
    if not yes:
        raise AssertionError(description)

check('PF_EMI_PATCH_LEVEL = "pf.24"' in s, 'Java patch level pf.24')
check('mod_version=1.1.23-pf.24' in (ROOT/'gradle.properties').read_text(), 'Gradle version pf.24')
check('pf.24' in (ROOT/'build.bat').read_text(), 'build.bat version pf.24')
check('PF-EMI-1.21.11-pf.24.jar' in (ROOT/'build.bat').read_text(), 'Fabric JAR name')
check('--no-daemon' in (ROOT/'build.bat').read_text(), 'non-persistent Gradle build')
check((ROOT/'APPLY_PF_EMI_PF_24.bat').exists(), 'incremented apply script')
check('call build.bat' in (ROOT/'APPLY_PF_EMI_PF_24.bat').read_text(), 'apply builds Fabric')
check('if errorlevel 1' in (ROOT/'build.bat').read_text().lower(), 'build failure remains non-success')

check('private static boolean isRetiredProjectFlintRecipe(Identifier id)' in s, 'native recipe retirement predicate')
check('&& PF.equals(id.getNamespace())' in s, 'retired recipes namespace scoped')
check('id.getPath().endsWith("_stripped_log_from_slabs")' in s, '10 wood variants covered by suffix')
retired = ('loose_dirt_from_slabs','loose_mud_bricks_from_slabs','sanded_wood_slabs_from_grinded','primitive_wooden_sheet_from_stripped_log','personal_pmi_sheets')
match = re.search(r'RETIRED_PROJECTFLINT_RECIPE_IDS = Set\.of\((.*?)\);', s, re.S)
check(match is not None, 'explicit retired recipe IDs')
for name in retired:
    check(f'"{name}"' in match.group(1), f'retired {name}')
check('if (isRetiredProjectFlintRecipe(id)) return true;' in s, 'native EMI recipe filtering')
check('if (isRetiredProjectFlintRecipe(id)) continue;' in s, 'native auto-remap filtering')
check('|| isRetiredProjectFlintRecipe(id)) continue;' in s, 'Hardwood type recipe filtering')
check('if (isRetiredProjectFlintRecipe(pfId(recipeId))) continue;' in s, 'bundled Hardwood data filtering')

# The hardcoded Clay + Wooden Grinder listings used different synthetic IDs
# from ProjectFlint's original JSON ID. Prevent manual re-registration.
match = re.search(r'private static void registerClayConversions\(.*?\n\t}\n\n\tprivate static void registerLumberProcessing',s,re.S)
check(match is not None, 'find shared Clay/Wooden Grinder mapping')
conversions = match.group(0)
for path in ('/loose_dirt_from_slabs','/loose_mud_block','/sanded_reverse'):
    check(f'prefix + "{path}"' not in conversions, f'bad duplicated Grinder route absent {path}')
check('if (isRetiredGrinderDisplay(id)) return;' in s, 'synthetic Grinder aliases guarded')
for path in ('loose_dirt_from_slabs','loose_mud_block','sanded_reverse'):
    check(f'path.endsWith("/{path}")' in s, f'guard synthetic alias {path}')
check('simple(registry, target, prefix + "/grinded_wood"' in conversions, 'Sanded -> Grinded forward retained')
check('simple(registry, target, prefix + "/loose_mud_slab"' in conversions, 'Loose Mud Brick -> Slab retained')
check('simple(registry, target, prefix + "/fireclay_slab_to_block"' in conversions, 'balanced Fireclay reverse retained')
check('simple(registry, target, prefix + "/mortar_block"' in conversions, 'balanced Mortar reverse retained')
check('simple(registry, target, prefix + "/crude_clay_block"' in conversions, 'balanced Crude Clay reverse retained')
check('simple(registry, category, prefix + "/" + id + "_slab", List.of(mc(log)), mc(slab, 2));' in s, 'forward log slab recipes retained')
check('"_log", List.of(mc(slab, 2))' not in s, 'log reverse not restored')
check('process(registry, wood, "wood/grinded_slabs_with_file"' in s, 'Wooden Grinder File conversion retained')
check('"wooden_chest_upgrade"' in s, 'Wooden Chest upgrade retained')
check('"placement/fill_empty_water_pot_clay_bucket"' in s, 'Water Pot fill retained')
check('DYNAMITE_YIELD_MULTIPLIER_03169 = 1.25' in s, 'v31.69 Dynamite multiplier unchanged')
check('registerHardwoodCrafting(registry)' in s, 'Hardwood recipe index remains')
check('registerStationTransferHandlers(registry)' in s, 'station recipe transfer remains')
check('ProjectFlintMultiblockPlugin.registerAll(registry)' in s, 'multiblocks retained')
check('0.32.3' in (ROOT/'README.md').read_text(), 'README updated')
check('PF-EMI 1.1.23-pf.24' in (ROOT/'CHANGELOG.md').read_text(), 'changelog updated')
check('15' in (ROOT/'docs/PF24_PROJECTFLINT_0323_RECIPE_RETIREMENT.md').read_text(), 'parity report includes 15 IDs')

print(f'PASS {len(checks)}/{len(checks)} source-contract checks')
if len(sys.argv)>1:
    zip_path = Path(sys.argv[1]); check(zip_path.exists(), 'ZIP exists')
    with zipfile.ZipFile(zip_path) as z:
        names=z.namelist()
        check(z.testzip() is None, 'ZIP CRC passes')
        check(len(set(names))==len(names),'no repeated ZIP paths')
        check(all(not n.startswith('/') and '..' not in Path(n).parts and '/' not in n[:1] for n in names),'source-relative paths')
        check(all(not n.startswith('pf24_source/') for n in names),'no parent folder')
        check('xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java' in names,'patch contains changed Java')
        check('APPLY_PF_EMI_PF_24.bat' in names,'patch contains apply script')
        for n in names:
            check(z.read(n)==(ROOT/n).read_bytes(),f'ZIP bytes match source: {n}')
    print(f'PASS ZIP checks ({len(names)} files)')
