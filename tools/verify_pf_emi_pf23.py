#!/usr/bin/env python3
"""Offline PF-EMI pf.23 source-contract verification (NOT a Minecraft build)."""
from pathlib import Path
import re, json, sys, zipfile
ROOT = Path(__file__).resolve().parents[1]
s = (ROOT/'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java').read_text(encoding='utf-8')
props = (ROOT/'gradle.properties').read_text(encoding='utf-8')
release = (ROOT/'docs/PF23_PROJECTFLINT_0321_PARITY.md').read_text(encoding='utf-8')
checks = []
def check(condition, label):
    checks.append(label)
    if not condition: raise AssertionError(label)
check('PF_EMI_PATCH_LEVEL = "pf.23"' in s, 'patch level')
check('mod_version=1.1.23-pf.23' in props, 'Gradle version')
check('pf.23' in (ROOT/'build.bat').read_text(), 'build script')
check((ROOT/'APPLY_PF_EMI_PF_23.bat').exists(), 'unique apply script')
check('Wooden Chest' in release, 'preserve chest route docs')
check('"wooden_chest_upgrade"' in s and 'pf("wooden_chest_frame")' in s, 'existing chest upgrade')
check('pf("woven_chest")' in s, 'woven chest catalyst')
check('"wood/woven_chest_frame"' in s and 'mc("leather", 2), pf("woven_wood", 4)' in s, 'Woven Chest Frame recipe')
check('"wood/wattle_window"' in s and 'pf("wattled_daub_bricks", 4)' in s and 'pf("wattled_daub_window", 3)' in s, 'three windows')
check('"wood/wattle_from_cane"' in s and 'mc("sugar_cane", 3), pf("branch", 2)' in s, 'new Wattle alternative')
check('"wood/water_bearing_pot"' in s and 'pf("woven_wood", 6), pf("clay_pot")' in s, 'Water Bearing Pot cost')
check('"wood/grinded_slabs_with_file"' in s and 'pf("sanded_wood_slab", 3)' in s, 'File grinding cost')
check('"wood/woven_wood_from_slabs"' in s and 'pf("grinded_wood_slab", 3), pf("woven_fan", 3)' in s, 'new Woven Wood conversion')
check('"wood/unfired_clay_bucket_bone_knife"' not in s, 'removed Wooden Grinder Knife route')
check('"wood/unfired_clay_bucket_frame"' in s, 'Wooden Grinder Frame route stays')
check('Unfired Clay Bucket (Bone Knife)' in release and 'registerBundledHardwoodRecipes' in s, 'Hardwood Knife route read from source')
check('Hardwood production' in release and '3 Hardwood' in release, 'Hardwood output x3 documented')
check('Wooden Cabinet' in release and '4 Cabinets' in release, 'Cabinet output x4 documented')
check('"/mortar_kiln_base", List.of(pf("mortar_bricks", 24))' in s, 'Mortar Kiln Base 24')
check('"/bamboo_block"' not in s, 'removed bamboo slab reverse')
check('"_log", List.of(mc(slab, 2))' not in s, 'removed log reverse recipes')
check('"_slab", List.of(mc(log))' in s, 'forward slab recipes retained')
check('"bookshelf/book"' in s and 'pf("sugarcane_roll", 8), mc("leather")' in s, 'Bookshelf book 1 leather')
check('"mutton_pie"' in s and 'mc("cooked_mutton", 4), mc("sugar", 2)' in s and 'pf("mutton_pie", 3)' in s, 'Mutton Pie balance')
check('pf("sweet_berry_jam", 4)' in s, 'Sweet Berry Jam output 4')
check('pf("strawberry_jam", 4)' in s, 'Strawberry Jam output 4')
check('inputs.add(mc("flint", 6));' in s, 'standard Advanced Kiln 6 flint')
check('"advanced/burnt_branch_block"' in s and 'mc("flint", 4)' in s, 'special Advanced Kiln unchanged')
check('"lumber/boards"' in s and 'List.of(pf("wooden_board", 2)), 290' in s, 'Lumber Board chopping+washing time')
check('"lumber/beams"' in s and 'List.of(pf("wooden_beam")), 290' in s, 'Lumber Beam chopping+washing time')
check('"placement/fill_empty_water_pot_clay_bucket"' in s, 'full bucket water pot route')
check('List.of(pf("clay_water_bucket")), List.of(pf("water_pot"))' in s and 'List.of(pf("clay_bucket")), 0' in s, 'bucket returned from Water Pot')
check('"placement/recover_string"' in s, 'retrieve String by hand')
check('"placement/projectflint_brush_sheep"' in s and 'maxDamageItem("brush", 512)' in s, 'Project Flint upgraded Brush')
check('"leaves_flint_cutters"' in s and 'pf("branch", 5)' in s, 'Flint Plant Cutters yield')
check('At least 1 Raw Leather from adult cows' in s, 'adult cow guaranteed leather')
check(s.count('2-3 extra Raw Leather from adults') == 2, 'black and brown bear leather')
check('Unqualified tools and bare hands cannot break ores.' in s, 'mining description corrected')
check('registerDynamiteBlasting' in s and 'DYNAMITE_YIELD_MULTIPLIER_03169 = 1.25' in s, 'preserve v31.69 dynamite 1.25x')
check('"wooden_chest_upgrade"' in s and '"placement/fill_empty_water_pot_clay_bucket"' in s, 'world interactions kept independent')
check('#17' in release and '#25' in release and 'not gameplay routes' in release, 'unapproved proposals excluded')
check('0.32.1' in release, 'target source version')
if __name__ == '__main__':
    print(f'PASS {len(checks)}/{len(checks)} static source checks for PF-EMI pf.23')
    if len(sys.argv)>1:
        zip_path=Path(sys.argv[1]); assert zip_path.exists()
        with zipfile.ZipFile(zip_path) as z:
            assert z.testzip() is None, 'CRC failure'
            names=z.namelist()
            assert len(names)==len(set(names))
            assert all(not n.startswith('/') and '..' not in Path(n).parts for n in names)
            assert all(n in {a.as_posix() for a in [Path('CHANGELOG.md'),Path('README.md'),Path('build.bat'),Path('gradle.properties'),Path('PATCH_MANIFEST.md'),Path('APPLY_PF_EMI_PF_23.bat')]} or n.startswith(('xplat/','docs/','tools/')) for n in names)
            assert 'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java' in names
            assert 'APPLY_PF_EMI_PF_23.bat' in names
            print('PASS patch ZIP CRC, flat source-relative paths and manifest,',len(names),'entries')
