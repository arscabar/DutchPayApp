"""Compare automatic names with selectable candidates, never treating candidates as accepted OCR."""
import argparse
import json
from pathlib import Path
import sys
from evaluate_names import evaluate, read, rowkey, norm

MONEY = ('unit', 'count', 'printedTotal', 'baseTotal', 'amountBased', 'includedDiscount')

def included(i, zero):
    return not i.get('includedDiscount', False) and i.get('baseTotal', i['unit'] * i['count']) >= (0 if zero else 1)

def compare(before, after, gold, zero):
    previous_rows, current_rows = read(before), read(after)
    old = {r['file']: r for r in previous_rows}
    new = {r['file']: r for r in current_rows}
    assert len(old) == len(previous_rows) and len(new) == len(current_rows), 'Duplicate filenames'
    assert old.keys() == new.keys(), 'Input receipt sets differ'
    changes, money, regressions, corrected, uncertain = [], [], [], [], []
    for file, r in new.items():
        assert 'error' not in r and 'error' not in old[file], file
        previous = old[file]['items']
        if r.get('total') != old[file].get('total'):
            money.append({'file': file, 'field': 'total', 'before': old[file].get('total'), 'after': r.get('total')})
        if len(previous) != len(r['items']):
            money.append({'file': file, 'field': 'item_count', 'before': len(previous), 'after': len(r['items'])})
        for n, i in enumerate(r['items']):
            prior = previous[n] if n < len(previous) else {}
            delta = {k: [prior.get(k), i.get(k)] for k in MONEY if prior.get(k) != i.get(k)}
            if delta:
                money.append({'file': file, 'row': n + 1, 'fields': delta})
            original = i.get('originalName', '')
            accepted = bool(original and original != i['name'])
            if accepted or prior.get('name') != i['name']:
                changes.append({'file': file, 'row': n + 1, 'baseline': prior.get('name'),
                    'original': original, 'selected': i['name'], 'automatic': accepted,
                    'candidates': i.get('nameCandidates', [])})
    coverage = dict(evaluated_rows=0, unscored_rows=0, candidate_only_hits=0, automatic_aligned_names=0,
                    selectable_name_upper_bound=0, selectable_strict_row_upper_bound=0)
    for g in read(gold)['receipts']:
        if 'items' not in g:
            continue
        file = g['file']
        a = [i for i in old[file]['items'] if included(i, zero)]
        b = [i for i in new[file]['items'] if included(i, zero)]
        if len(a) != len(b) or len(a) != len(g['items']):
            uncertain.append({'file': file, 'reason': 'Row counts differ; manual alignment required'})
            coverage['unscored_rows'] += len(g['items'])
            continue
        for n, (e, x, y) in enumerate(zip(g['items'], a, b)):
            coverage['evaluated_rows'] += 1
            expected = norm(e['name'])
            candidates = {norm(s) for s in y.get('nameCandidates', [])}
            choices = candidates | {norm(y['name']), norm(y.get('originalName', ''))}
            coverage['candidate_only_hits'] += expected in candidates
            coverage['selectable_name_upper_bound'] += expected in choices
            coverage['selectable_strict_row_upper_bound'] += expected in choices and rowkey(e)[1:] == rowkey(y)[1:]
            was, now = norm(x['name']) == expected, norm(y['name']) == expected
            coverage['automatic_aligned_names'] += now
            if was != now:
                (regressions if was else corrected).append({'file': file, 'row': n + 1,
                    'gold': e['name'], 'before': x['name'], 'after': y['name']})
    return {'baseline': evaluate(before, gold, zero), 'current': evaluate(after, gold, zero),
        'candidate_coverage': coverage, 'correct_to_wrong': regressions, 'wrong_to_correct': corrected,
        'name_changes_all_receipts': changes, 'money_changes_all_receipts': money,
        'alignment_unscored': uncertain}

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--evidence', type=Path, default=Path(__file__).resolve().parents[1] / 'evidence')
    p.add_argument('--baseline', default='structure-app')
    p.add_argument('--current', default='names-app')
    args = p.parse_args()
    result = {'notes': [
        'Candidate coverage is an upper bound before user selection, not automatic OCR accuracy.',
        'Gold-aligned coverage/regressions use printed row order when row counts agree; inspect money/row changes.',
        'Automatic metrics reuse evaluate_names: NFC/compact name-stream CER and multiset exact names/rows.',
        'Fixed development samples: 12 receipts/34 positive rows plus 03.jpg/19 rows including free options.',
        'All automatic changes are listed, including receipts without name gold; unlabelled changes are not scored.'
    ]}
    for suffix, gold, zero in [('63', 'document-scans-amounts.json', False), ('11', 'STRUCTURE_GOLD.json', True)]:
        result[suffix] = compare(args.evidence / 'raw' / f'{args.baseline}{suffix}.json',
            args.evidence / 'raw' / f'{args.current}{suffix}.json', args.evidence / gold, zero)
    sys.stdout.reconfigure(encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False, indent=2))
