"""Score each printed field; unknown unit prices are unscored, never inferred gold."""
import argparse
import json
import sys
from evaluate_names import read, norm, distance, names
from evaluate_menu_align import align, expected, actual, selected, joined_cells

def evaluate(input_path, gold_path, section='items', kind='positive'):
    raw = read(input_path)
    by = {r['file']: r for r in raw}
    assert len(by) == len(raw), 'Duplicate filenames'
    fields = {k: {'correct': 0, 'total': 0} for k in expected({})}
    totals = dict(rows=0, output_rows=0, missing_rows=0, extra_rows=0,
        complete_observed_rows=0, four_field_rows=0, complete_four_field_rows=0,
        unprinted_unit_exposed=0, total_correct=0, total_scored=0, cer_edits=0, cer_units=0)
    details, layouts = [], []
    for g in read(gold_path)['receipts']:
        if section not in g:
            continue
        r = by[g['file']]
        assert 'error' not in r, r.get('error')
        refs = [i for i in g[section] if not i.get('exclude', False)]
        out = [i for i in r['items'] if selected(i, kind)]
        pairs, extras = align(refs, out)
        layouts += [{'file':g['file'], **v} for v in joined_cells(refs,out)]
        reftext, text = '\n'.join(names(refs)), '\n'.join(names(out))
        totals['cer_edits'] += distance(reftext, text); totals['cer_units'] += len(reftext)
        totals['output_rows'] += len(out); totals['extra_rows'] += len(extras)
        target = g.get('total', g.get('expectedTotal'))
        if target is not None:
            totals['total_scored'] += 1; totals['total_correct'] += r.get('total') == target
        for n, j in pairs:
            e, a = expected(refs[n]), actual(out[j]) if j is not None else {}
            known = [k for k in fields if e[k] is not None]
            passed = {k: j is not None and (norm(a[k]) == norm(e[k]) if k == 'name' else a[k] == e[k]) for k in known}
            for k in known:
                fields[k]['total'] += 1; fields[k]['correct'] += passed[k]
            complete = all(passed.values()) and j is not None
            totals['rows'] += 1; totals['missing_rows'] += j is None
            totals['complete_observed_rows'] += complete
            totals['four_field_rows'] += len(known) == 4
            totals['complete_four_field_rows'] += len(known) == 4 and complete
            totals['unprinted_unit_exposed'] += e['unit'] is None and a.get('unit') is not None
            details.append({'file': g['file'], 'gold_row': n+1, 'output_row': None if j is None else j+1,
                'expected': e, 'actual': a, 'incorrect': [k for k in known if not passed[k]],
                'role': refs[n].get('role'), 'warning': '' if j is None else out[j].get('warning', '')})
        for j in extras:
            details.append({'file': g['file'], 'extra_output_row': j+1, 'actual': actual(out[j])})
    roles = {}
    for role in sorted({r.get('role') for r in details} - {None}):
        group = [r for r in details if r.get('role') == role]
        roles[role] = {'rows': len(group), 'complete': sum(not r['incorrect'] for r in group),
            'fields': {k: {'correct': sum(r['expected'][k] is not None and k not in r['incorrect'] for r in group),
                'total': sum(r['expected'][k] is not None for r in group)} for k in fields}}
    return dict(input=str(input_path), gold=str(gold_path), section=section, kind=kind,
        fields=fields, totals=totals, roles=roles, layout_suspects=layouts, rows=details)

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--input', required=True); p.add_argument('--gold', required=True)
    p.add_argument('--section', default='items', choices=['items','zero_rows','negative_rows']); p.add_argument('--kind', default='positive', choices=['positive','zero','nonnegative','removed'])
    args = p.parse_args(); sys.stdout.reconfigure(encoding='utf-8')
    print(json.dumps(evaluate(args.input,args.gold,args.section,args.kind),ensure_ascii=False,indent=2))
