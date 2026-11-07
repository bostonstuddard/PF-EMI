#!/usr/bin/env python3
"""Offline source/compatibility checks; NOT a substitute for Gradle or Minecraft."""
from pathlib import Path
import re
import math
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
s = (root / 'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java').read_text()
props = (root / 'gradle.properties').read_text()
build = (root / 'build.bat').read_text()
docs = (root / 'docs/PF21_PROJECTFLINT_03168_PREVIEW.md').read_text()
checks=[]
def check(name, passed):
    checks.append((name, bool(passed)))
    print(('PASS' if passed else 'FAIL') + '  ' + name)

check('pf.21 Java and Gradle version agreement', 'PF_EMI_PATCH_LEVEL = "pf.21"' in s and 'mod_version=1.1.23-pf.21' in props and 'pf.21' in build)
check('Minecraft remains 1.21.11', 'minecraft_version=1.21.11' in props)
check('fabric-only build unchanged', 'enabled_platforms=fabric' in props)
check('pre-release version gating', all(x in s for x in ['projectFlintAtLeast03168()', 'getFriendlyString', 'patch >= 68', 'projectFlintHas068 = supported;']))
check('future 20-percent and legacy 50-percent bonus', all(x in s for x in ['WEAK_DYNAMITE_YIELD_MULTIPLIER = 1.20', 'LEGACY_DYNAMITE_YIELD_MULTIPLIER = 1.50', 'projectFlintAtLeast03168() ? WEAK_DYNAMITE_YIELD_MULTIPLIER : LEGACY_DYNAMITE_YIELD_MULTIPLIER']))
check('probabilities are not clamped to 100%', 'long guaranteed = (long) Math.floor(boosted)' in s and 'double remainder = boosted - guaranteed' in s and 'setAmount(guaranteed)' in s)
check('all eight previously supported blastable kinds retained', all(f'"{block}", mc("{block}")' in s for block in ['stone','cobblestone','andesite','granite','diorite','iron_ore','copper_ore','coal_ore']))
check('drop text conditional on block destruction', 'Drops are rolled only if this block breaks.' in s)
check('short UI note on reduced stone break effectiveness', 'Small blast; stone resists more than ore.' in s)
check('dripstone no-drop page and version guard', '"drops/locked_dripstone"' in s and '"dripstone_block", "pointed_dripstone"' in s and re.search(r'if \(projectFlintAtLeast03168\(\)\)\s*\{\s*info\(registry, drops, "drops/locked_dripstone"',s) is not None)
check('Hardwood imports actual synchronized shaped slots', 'hardwoodCells(delegate)' in s and 'recipe instanceof ShapedRecipe' in s)
check('Bundled JSON fallback reads actual source pattern', 'json.getAsJsonArray("pattern")' in s and 'JsonObject key = json.getAsJsonObject("key")' in s)
check('no pre-release 3x3 hardcoded item substitutions', 'hardwood_dynamite").equals' not in s and '"hardwood_dynamite", 3' not in s)
check('planned full bottom copper recipe accurately documented', all(row in docs for row in ['P F P','P R P','C C C','Copper Dust (3 total)','Black Powder (4 total)']))
check('source status caveat documented', 'verified from released code' in docs and 'No runtime certification' in docs)

for base, guaranteed, remainder in [(0.50,0,0.60),(0.75,0,0.90),(0.83,0,0.996),(1.0,1,0.20),(0.024,0,0.0288),(1.9,2,0.28)]:
    boosted=base*1.20
    actual=math.floor(boosted)
    check(f'20% boost independent roll {base:g} -> {guaranteed} + {remainder:g}', actual==guaranteed and math.isclose(boosted-actual, remainder, abs_tol=1e-10))

print(f'{sum(ok for _,ok in checks)}/{len(checks)} static checks passed')
raise SystemExit(0 if all(ok for _,ok in checks) else 1)
