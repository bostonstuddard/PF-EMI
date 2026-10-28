#!/usr/bin/env python3
"""Offline PF-EMI pf.12 static regression checks.

This checks local source contracts, not compiler or gameplay behavior.
"""
from pathlib import Path
import collections
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
PKG = ROOT / 'xplat/src/main/java/dev/emi/emi'
PLUGIN = (PKG/'ProjectFlintPlugin.java').read_text(encoding='utf8')
INDEX = (PKG/'ProjectFlintRecipeIndex.java').read_text(encoding='utf8')
LOOT = (PKG/'recipe/ProjectFlintLootRecipe.java').read_text(encoding='utf8')
BLOCKS = (PKG/'ProjectFlintMultiblockPlugin.java').read_text(encoding='utf8')
CHECKS = {
  'mod version': 'mod_version=1.1.23-pf.12' in (ROOT/'gradle.properties').read_text(),
  'plugin version': 'PF_EMI_PATCH_LEVEL = "pf.12"' in PLUGIN,
  'Forge geometry': all(t in BLOCKS for t in ['"3x3x6 (+ open chimney)"','material(brick, 28','material(stairs, 4','material(slab, 4','"Stair crown"','"Slab caps"']),
  'Well frame': all(t in BLOCKS for t in ['"3x3x8"','material(bricks, 33','material(shingles, 7','material(beam, 6','material(rope, 2']),
  'component-bound parchment': all(t in PLUGIN for t in ['DataComponentTypes.CUSTOM_DATA', 'NbtComponent.of(data)', 'ProjectFlintRecipe', 'DataComponentTypes.CUSTOM_MODEL_DATA']),
  'staged bacon research': 'bookshelf/research/finish_bacon_and_eggs' in PLUGIN and 'boundParchment("bacon_and_eggs_unfinished")' in PLUGIN,
  'stove bound id': 'List.of(boundParchment(recipeId))' in PLUGIN,
  'tanned armor grind': all(f'"clay/tanned_{name}"' in PLUGIN for name in ('helmet','chestplate','leggings','boots')),
  'fireclay/brick grind': all(f'"clay/{name}"' in PLUGIN for name in ('fireclay_brick_block','fireclay_brick_slab','fireclay_brick_stairs','fireclay_slab_to_block','unfired_brick_recycle')),
  '512-use leather armor': all(f'"wood/leather_{name}"' in PLUGIN for name in ('helmet','chestplate','leggings','boots')) and 'stack.set(DataComponentTypes.MAX_DAMAGE, maximum)' in PLUGIN,
  'campfire heat eligibility': all(f'prefix, "{name}"' in PLUGIN for name in ('burnt_twig','burnt_branch','clay_bucket','brick','fireclay_brick')),
  'nonconsumed searchable mob identity': 'searchable.add(subject);' in LOOT and 'Spawn eggs referenced by the mob-loot guide stay searchable' in INDEX,
  'fabric build target': 'enabled_platforms=fabric' in (ROOT/'gradle.properties').read_text(),
}
# Brace/parentheses lexer strips literals and comments, then checks balanced tokens.
for file in [PKG/'ProjectFlintPlugin.java', PKG/'ProjectFlintRecipeIndex.java',
             PKG/'ProjectFlintMultiblockPlugin.java', PKG/'recipe/ProjectFlintLootRecipe.java',
             PKG/'widget/ProjectFlintMultiblockWidget.java', PKG/'recipe/ProjectFlintMultiblockRecipe.java']:
    src = file.read_text(encoding='utf8')
    clean = re.sub(r'/\*.*?\*/|//[^\n]*|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', '',src, flags=re.S)
    stack=[]; pairs={'(':')','[':']','{':'}'}
    valid=True
    for c in clean:
        if c in pairs:stack.append(c)
        elif c in pairs.values():
            if not stack or pairs[stack.pop()] != c:
                valid=False;break
    CHECKS[f'balanced {file.name}']= valid and not stack

# Literal recipe IDs (not dynamic loop IDs) should not collide within this source.
# Exclude modern research loops whose runtime IDs have unique prefixes.
ids=re.findall(r'\b(?:simple|process|world|info|blockDrop|mob|stoveRecipe)\(registry,\s*(?:\w+,\s*)?"([^"+]+)"',PLUGIN)
collisions={name:n for name,n in collections.Counter(ids).items() if n>1 and not name.endswith('/')}
CHECKS['no literal recipe ID collisions']=not collisions

for label, ok in CHECKS.items():print(('PASS' if ok else 'FAIL')+'  '+label)
if collisions:print('Duplicate literal recipe IDs:',collisions)
print(f'Checks: {sum(CHECKS.values())}/{len(CHECKS)} passed')
sys.exit(0 if all(CHECKS.values()) else 1)
