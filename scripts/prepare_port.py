#!/usr/bin/env python3
"""Stage copies of the user-provided 1.21.11 sources; never modify the old project."""
from pathlib import Path
import argparse
import shutil
import json

HERE = Path(__file__).resolve().parent
PORT = HERE.parent
ROOT = PORT.parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--reset', action='store_true', help='Delete/recreate prior staged 26.x Java files (loses migration work).')
args = parser.parse_args()
assert (ROOT / 'xplat/src/main/java').is_dir(), f'Expected original 1.21.11 xplat at {ROOT}'

out_java = PORT / 'src/main/java'
if out_java.exists() and any(out_java.rglob('*.java')) and not args.reset:
	raise SystemExit('Port source tree already exists; refusing to overwrite manual changes. Pass --reset intentionally.')
if args.reset and out_java.exists():
	shutil.rmtree(out_java)
out_java.mkdir(parents=True, exist_ok=True)
seen = set()
for module in ('xplat','fabric'):
	for src in (ROOT/module/'src/main/java').rglob('*.java'):
		rel = src.relative_to(ROOT/module/'src/main/java')
		if module == 'xplat' and (str(rel).startswith('dev/emi/emi/jemi/') or str(rel).startswith('dev/emi/emi/mixin/jei/')):
			continue
		if module == 'fabric' and str(rel) == 'dev/emi/emi/compat/EmiModMenu.java':
			continue
		if str(rel) in seen:
			raise SystemExit(f'Duplicate source path: {rel}')
		seen.add(str(rel))
		dest = out_java/rel
		dest.parent.mkdir(parents=True,exist_ok=True)
		shutil.copy2(src, dest)

out_res = PORT/'src/main/resources'
for module in ('xplat','fabric'):
	for src in (ROOT/module/'src/main/resources').rglob('*'):
		if not src.is_file():
			continue
		rel = src.relative_to(ROOT/module/'src/main/resources')
		if str(rel) in ('fabric.mod.json', 'pack.mcmeta', 'emi.accesswidener') or str(rel).endswith('-refmap.json'):
			continue
		dest = out_res/rel
		if dest.exists():
			continue
		dest.parent.mkdir(parents=True,exist_ok=True)
		shutil.copy2(src,dest)
for cfg in out_res.glob('*.mixins.json'):
	try:
		data = json.loads(cfg.read_text(encoding='utf-8'))
		data.pop('refmap', None)  # 26.1+ ships unobfuscated Minecraft classes.
		data['compatibilityLevel'] = 'JAVA_21'  # Mixins supports up to JAVA_21; bytecode targets Java 25 via Gradle.
		cfg.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
	except ValueError as e:
		raise SystemExit(f'Invalid mixin JSON in {cfg}: {e}')
# Separate NeoForge adapter source tree, since Fabric and NeoForge use different loader APIs.
neo_root = PORT/'neoforge'
neo_java = neo_root/'src/main/java'
if neo_java.exists() and any(neo_java.rglob('*.java')) and not args.reset:
	raise SystemExit('NeoForge staging already exists; refusing to overwrite manual changes.')
if args.reset and neo_java.exists():
	shutil.rmtree(neo_java)
for module in ('xplat','neoforge'):
	for src in (ROOT/module/'src/main/java').rglob('*.java'):
		rel = src.relative_to(ROOT/module/'src/main/java')
		if module == 'xplat' and (str(rel).startswith('dev/emi/emi/jemi/') or str(rel).startswith('dev/emi/emi/mixin/jei/')):
			continue
		dest = neo_java/rel
		if dest.exists():
			raise SystemExit(f'Duplicate NeoForge Java path: {rel}')
		dest.parent.mkdir(parents=True,exist_ok=True)
		shutil.copy2(src,dest)

for module in ('xplat','neoforge'):
	for src in (ROOT/module/'src/main/resources').rglob('*'):
		if not src.is_file():
			continue
		rel = src.relative_to(ROOT/module/'src/main/resources')
		if str(rel) in ('fabric.mod.json', 'pack.mcmeta', 'emi.accesswidener', 'META-INF/neoforge.mods.toml') or str(rel).endswith('-refmap.json'):
			continue
		dest = neo_root/'src/main/resources'/rel
		if dest.exists():
			continue
		dest.parent.mkdir(parents=True,exist_ok=True)
		shutil.copy2(src,dest)

for cfg in (neo_root/'src/main/resources').glob('*.mixins.json'):
	data = json.loads(cfg.read_text(encoding='utf-8'))
	data.pop('refmap',None)
	data['compatibilityLevel'] = 'JAVA_21'
	cfg.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')

# Independent Forge staging (kept apart from Fabric / NeoForge loader code).
forge_root = PORT / 'forge'
forge_java = forge_root / 'src/main/java'
if forge_java.exists() and any(forge_java.rglob('*.java')) and not args.reset:
    raise SystemExit('Forge staging already exists; refusing to overwrite manual changes.')
if args.reset and forge_java.exists():
    shutil.rmtree(forge_java)
forge_count = 0
for module in ('xplat','forge'):
    for src in (ROOT/module/'src/main/java').rglob('*.java'):
        rel = src.relative_to(ROOT/module/'src/main/java')
        if module == 'xplat' and (str(rel).startswith('dev/emi/emi/jemi/') or str(rel).startswith('dev/emi/emi/mixin/jei/')):
            continue
        dest = forge_java / rel
        if dest.exists():
            raise SystemExit(f'Duplicate Forge Java path: {rel}')
        dest.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(src,dest)
        forge_count += 1
for module in ('xplat','forge'):
    for src in (ROOT/module/'src/main/resources').rglob('*'):
        if not src.is_file():
            continue
        rel = src.relative_to(ROOT/module/'src/main/resources')
        if str(rel) in ('fabric.mod.json', 'pack.mcmeta', 'emi.accesswidener', 'META-INF/mods.toml') or str(rel).endswith('-refmap.json'):
            continue
        dest = forge_root/'src/main/resources'/rel
        if dest.exists():
            continue
        dest.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(src,dest)
for cfg in (forge_root/'src/main/resources').glob('*.mixins.json'):
    data = json.loads(cfg.read_text(encoding='utf-8'))
    data.pop('refmap',None)
    data['compatibilityLevel']='JAVA_21'
    cfg.write_text(json.dumps(data,indent=2)+'\n', encoding='utf-8')

print(f'Staged Fabric {len(seen)} / NeoForge {len(list(neo_java.rglob("*.java")))} / Forge {forge_count} Java files (still Yarn names; migrate before building).')
print('Retained original 1.21.11 project without modification.')
print('IMPORTANT: Mixin targets and access widener must be migrated/reviewed manually.')
