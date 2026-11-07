#!/usr/bin/env python3
"""PF-EMI pf.22 source-contract checks; not Gradle or a Minecraft playtest."""
from pathlib import Path
import re, math, sys, zipfile, json
root = Path(__file__).resolve().parents[1]
s=(root/'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java').read_text()
props=(root/'gradle.properties').read_text()
build=(root/'build.bat').read_text()
doc=(root/'docs/PF22_PROJECTFLINT_0.31.69_PARITY.md').read_text()
checks=[]
def ck(label,cond):
    checks.append((label,bool(cond)))
    if not cond: print('FAIL:',label)
    else: print('PASS:',label)
ck('pf22 versions coherent',all(v in u for v,u in [('PF_EMI_PATCH_LEVEL = "pf.22"',s),('mod_version=1.1.23-pf.22',props),('pf.22',build)]))
ck('Fabric MC1.21.11 target retained',all(x in props for x in ['enabled_platforms=fabric','minecraft_version=1.21.11']))
ck('world upgrade correctly consumes the Wooden Frame',bool(re.search(r'world\(registry, "wooden_chest_upgrade",\s*List\.of\(pf\("wooden_chest_frame"\)\)',s)))
ck('Woven Chest is a non-consumed target',bool(re.search(r'"wooden_chest_upgrade",\s*List\.of\(pf\("wooden_chest_frame"\)\), List\.of\(pf\("woven_chest"\)\), pf\("wooden_chest"\)',s)))
ck('Hardwood 3x3 imported via source type',all(x in s for x in ['registerHardwoodCrafting', 'hardwoodCells(delegate)','registerBundledHardwoodRecipes','hardwood_shaped','bundledCells(json, type)']))
ck('Hardwood output, matrix and count documented',all(x in doc for x in ['DDD','DWD','TTT','Wood Dust (five total)','Tannin (three across the bottom)','Output = 1 Hardwood']))
ck('Water Bucket remainder is implemented',bool(re.search(r'"water_bucket"\)\.equals\(option\.getId\(\)\)\)\s*\{\s*option\.setRemainder\(mc\("bucket"\)\)',s)))
ck('Hardwood personal 2x2 recipe retained', '"hardwood"' in s and 'registerPersonalCrafting' in s)
ck('version detection using Fabric mod metadata',all(x in s for x in ['projectFlintPatch()', 'getFriendlyString', 'projectFlintAtLeast03169()', 'projectFlintAtLeast03168()']))
ck('1.25x blast multiplier', 'DYNAMITE_YIELD_MULTIPLIER_03169 = 1.25' in s)
ck('1.20x v68 multiplier retained', 'DYNAMITE_YIELD_MULTIPLIER_03168 = 1.20' in s)
ck('1.50x legacy multiplier retained', 'LEGACY_DYNAMITE_YIELD_MULTIPLIER = 1.50' in s)
ck('blast version prioritizes v69',bool(re.search(r'if \(projectFlintAtLeast03169\(\)\) return DYNAMITE_YIELD_MULTIPLIER_03169;\s*if \(projectFlintAtLeast03168\(\)\)',s)))
ck('dynamite 3-second fuse and 4 ore/2 stone limits shown',all(x in s for x in ['3-second fuse; up to 4 ores and 2 stone blocks.','Ore: 3 blocks. Stone: 2.25 blocks; 65% break chance.']))
ck('v68 fuse/limits remain accurate',all(x in s for x in ['4.5-second fuse; up to 2 ores and 1 stone block.','Ore: 1.75 blocks. Stone: 1.25 blocks; 50% break chance.']))
ck('stone individual break notes only in v69', '65% break chance within 2.25 blocks; max 2 rocks.' in s)
ck('ore individual notes only in v69', 'Within 3 blocks; max 4 ore blocks per blast.' in s)
ck('blasting probabilities keep overrolls',all(x in s for x in ['Math.floor(boosted)','double remainder = boosted - guaranteed','setAmount(guaranteed)']))
ck('mining recovery for mud blocks version-gated',bool(re.search(r'if \(projectFlintAtLeast03169\(\)\) \{\s*EmiIngredient masonryPickaxes.*?List\.of\("packed_mud", "mud_bricks"\)',s,re.S)))
ck('mud recovery uses pickaxes only', 'any(1, mc("wooden_pickaxe"), pf("hardwood_pickaxe"))' in s)
ck('advanced geology locked to correct future tiers', '"drops/advanced_geology"' in s and '"tuff", "calcite"' in s)
ck('No hardcoded pre-release Hardwood fake recipe', 'grid(registry, hardwood, "hardwood/planned' not in s)
ck('no future gameplay source in patch',not any('dev/projectflint/' in str(f) for f in root.rglob('*.java')))
ck('apply BAT uses pf22 and existing source guard',all(x in (root/'APPLY_PF_EMI_PF_22.bat').read_text() for x in ['PF-EMI','build.bat','settings.gradle']))
for base in [0.024,.5,.66,.75,.83,1.0,1.2,1.5]:
    x=base*1.25; n=math.floor(x); r=x-n
    ck(f'1.25x probability {base:g} -> guaranteed {n} + remainder {r:.4g}', math.isclose(n+r, x,abs_tol=1e-12) and (0<=r<1))
ck('release notes explain lack of gameplay changes', 'does **not** modify ProjectFlint gameplay' in doc)
print(f'\n{sum(ok for _,ok in checks)}/{len(checks)} source checks passed')
sys.exit(0 if all(ok for _,ok in checks) else 1)
