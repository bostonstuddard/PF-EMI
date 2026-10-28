#!/usr/bin/env python3
"""Static PF-EMI coverage inventory against an extracted ProjectFlint source checkout.

This is NOT an authority for runtime recipes. Its purpose is to make omissions
visible for human review and to track registrations across later updates.
Usage: python tools/audit_projectflint_emi.py /path/to/ProjectFlint --output docs/PF_SOURCE_AUDIT.md
"""
import argparse
import re
from pathlib import Path
from datetime import date

EMI_DIR = Path(__file__).resolve().parents[1]
PFJAVA = EMI_DIR / 'xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java'


def mapping(path):
    text = path.read_text(encoding='utf8')
    return dict(re.findall(r'public\s+static\s+final\s+\w+\s+(\w+)\s*=\s*register(?:BlockItem)?\("([a-z0-9_]+)"', text))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('projectflint', type=Path, help='Root of a current ProjectFlint checkout')
    parser.add_argument('--output', type=Path, default=Path('docs/PF_SOURCE_AUDIT.md'))
    parser.add_argument('--strict', action='store_true', help='Exit nonzero for missing source recipe outputs')
    args = parser.parse_args()
    source = args.projectflint / 'src/main/java/dev/projectflint'
    items = source / 'registry/ModItems.java'
    if not items.is_file():
        parser.error(f'Expected ProjectFlint source: {items}')
    symbol_map = mapping(items)
    emi_text = PFJAVA.read_text(encoding='utf8')
    literal_ids = set(re.findall(r'\bpf\("([a-z0-9_]+)"', emi_text))
    literal_ids.update(re.findall(r'\bpfStack\("([a-z0-9_]+)"', (EMI_DIR/'xplat/src/main/java/dev/emi/emi/ProjectFlintMultiblockPlugin.java').read_text()))
    # This regex cannot infer constructed IDs, aliases, tags or item components.
    dynamic_ids = sorted(set(re.findall(r'"([a-z0-9_]+)"', emi_text)))
    keys = sorted(set(symbol_map.values()))
    outputs = set()
    origin = {}
    for path in source.rglob('*.java'):
        code = path.read_text(encoding='utf8', errors='replace')
        for symbol in re.findall(r'new\s+ItemStack\(\s*ModItems\.(\w+)',code):
            if symbol in symbol_map:
                outputs.add(symbol_map[symbol])
                origin.setdefault(symbol_map[symbol],set()).add(str(path.relative_to(source)))
    uncovered_outputs = sorted(x for x in outputs if x not in literal_ids and x not in dynamic_ids)
    not_referenced = sorted(x for x in keys if x not in literal_ids and x not in dynamic_ids)
    lines = [
        '# ProjectFlint to PF-EMI automated reference audit', '',
        f'- Date: {date.today().isoformat()}',
        f'- Local ProjectFlint root: `{args.projectflint}`',
        f'- Registered ProjectFlint items parsed: **{len(keys)}**',
        f'- ProjectFlint output item IDs found in `new ItemStack(ModItems.*)` expressions: **{len(outputs)}**',
        f'- PF-EMI literal `pf(...)` item IDs: **{len(literal_ids)}**',
        f'- Outputs requiring manual review: **{len(uncovered_outputs)}**',
        f'- Other registered items with no direct literal reference: **{len(not_referenced)}**', '',
        '> IMPORTANT: `not referenced` does not necessarily mean missing gameplay. Iterated IDs, aliases, tags,',
        '> native recipe manager recipes, use of `Items.*`, and runtime registration are not resolved by this script.',
        '> Likewise `referenced` does not prove a correct count, workstation, time, input, byproduct, or tool.', '',
        '## Registered source output IDs to review', '',
    ]
    for item in sorted(outputs):
        lines.append(f'- [ ] `{item}` — PF-EMI literal reference: {"yes" if item in literal_ids else "not found directly"}; producer(s): {", ".join(sorted(origin[item])[:4])}')
    lines += ['', '## All ProjectFlint registered item IDs', '']
    for item in keys:
        lines.append(f'- [ ] `{item}` — check production, consumption, tool, block drops and player-facing EMI visibility (literal PF-EMI reference: {"yes" if item in literal_ids else "not found directly"})')
    lines += ['', '## Follow-up manual test', '',
        '- [ ] Open EMI on every newly registered item; verify producing/consuming recipes and counts.',
        '- [ ] Compare recipe variants and accepted tag inputs against the actual `Ingredient` objects.',
        '- [ ] Check non-crafting ways to obtain every item (harvest, mobs, breaking, containers, processing).',
        '- [ ] Reconcile page ID/category naming and duplicate recipes.',
        '- [ ] Confirm correct client/server runtime behavior with a current PF/EMI test environment.',
    ]
    dest = args.output if args.output.is_absolute() else EMI_DIR / args.output
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text('\n'.join(lines)+'\n',encoding='utf8')
    print(f'Wrote {dest} | {len(keys)} source items | {len(uncovered_outputs)} uncovered literal output refs')
    raise SystemExit(1 if args.strict and uncovered_outputs else 0)


if __name__ == '__main__':
    main()
