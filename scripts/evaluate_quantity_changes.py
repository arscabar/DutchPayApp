"""Record quantity changes against the preserved v0.6 app output; never alter gold."""
import argparse
import json
from pathlib import Path


def load(path):
    return json.loads(Path(path).read_text('utf-8'))


def evaluate(current, baseline, gold):
    old = {r['file']: r for r in baseline}
    expected = {r['file']: r['rows'] for r in gold['records']}
    changes, structure, errors = [], [], []
    fields = ('name', 'count', 'quantityKnown', 'unit', 'printedTotal', 'baseTotal', 'amountBased')
    for receipt in current:
        name = receipt['file']
        if receipt.get('error') or name not in old:
            errors.append({'file': name, 'error': receipt.get('error', 'missing baseline')})
            continue
        before, after = old[name].get('items', []), receipt.get('items', [])
        if len(before) != len(after):
            structure.append({'file': name, 'before': len(before), 'after': len(after),
                              'before_rows': [{k: row.get(k) for k in fields} for row in before],
                              'after_rows': [{k: row.get(k) for k in fields} for row in after]})
        for index, (a, b) in enumerate(zip(before, after), 1):
            if all(a.get(k) == b.get(k) for k in ('count', 'quantityKnown')):
                continue
            stable = len(before) == len(after) and a.get('printedTotal') == b.get('printedTotal')
            truth = expected.get(name, {}).get(str(index)) if stable else None
            clues = [d for d in receipt.get('numericDiagnostics', []) if d.get('accepted')
                     and str(d.get('printedAmount', '')).replace(',', '') == str(b.get('printedTotal'))]
            review = []
            if not stable:
                review.append('Positional comparison needs review: row count or printed amount changed')
            if truth is not None and (not b.get('quantityKnown') or b.get('count') != truth):
                review.append('Does not match quantity transcribed from the original receipt')
            if a.get('quantityKnown'):
                review.append('Previously known quantity changed; inspect original receipt')
            if not b.get('quantityKnown'):
                review.append('Quantity became unknown')
            changes.append({'file': name, 'row': index, 'same_printed_row': stable,
                            'before': {k: a.get(k) for k in fields},
                            'after': {k: b.get(k) for k in fields},
                            'reviewed_expected': truth,
                            'matching_amount_diagnostics': clues,
                            'review': review})
    return {'images': len(current), 'baseline_images': len(baseline), 'changes': changes,
            'changed_rows': len(changes), 'structural_changes': structure, 'errors': errors,
            'note': 'Rows are compared by position. Matching amounts are supporting clues, not unique row IDs.'}


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('current', type=Path)
    p.add_argument('--baseline', type=Path, default=Path('evidence/raw/menus-app63.json'))
    p.add_argument('--gold', type=Path, default=Path('evidence/MENU_QUANTITY_REVIEW.json'))
    p.add_argument('--output', type=Path, default=Path('evidence/raw/quantity-changes-final.json'))
    args = p.parse_args()
    result = evaluate(load(args.current), load(args.baseline), load(args.gold))
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    summary = {k: v for k, v in result.items() if k not in ('changes', 'structural_changes')}
    summary['structural_changes'] = [{k: v for k, v in r.items() if not k.endswith('_rows')}
                                     for r in result['structural_changes']]
    print(json.dumps(summary, ensure_ascii=False, indent=2))
