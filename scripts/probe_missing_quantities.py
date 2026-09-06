"""Score actual Android quantity output against the manually reviewed rows."""
import argparse
import json
from pathlib import Path


def evaluate(raw, gold):
    by_file = {r['file']: r for r in raw}
    rows, violations = [], []
    for receipt in raw:
        before, after = receipt['before'], receipt
        if before.get('total') != after.get('total') or len(before['items']) != len(after['items']):
            violations.append({'file': receipt['file'], 'kind': 'total_or_row_count'})
        for index, (a, b) in enumerate(zip(before['items'], after['items']), 1):
            if any(a.get(k) != b.get(k) for k in ('name', 'printedTotal', 'includedDiscount')):
                violations.append({'file': receipt['file'], 'row': index, 'kind': 'protected_field'})
            if a.get('quantityKnown', True) and any(a.get(k) != b.get(k) for k in ('unit', 'count', 'quantityKnown')):
                violations.append({'file': receipt['file'], 'row': index, 'kind': 'known_quantity'})
    for record in gold['records']:
        actual = by_file.get(record['file'])
        for number, expected in record['rows'].items():
            index = int(number) - 1
            before = actual['before']['items'][index] if actual and index < len(actual['before']['items']) else {}
            after = actual['items'][index] if actual and index < len(actual['items']) else {}
            known = after.get('quantityKnown', False)
            rows.append({'file': record['file'], 'row': int(number), 'expected': expected,
                         'actual': after.get('count'), 'known': known,
                         'correct': known and after.get('count') == expected,
                         'newly_read': not before.get('quantityKnown', False) and known})
    return {'images': len(raw), 'reviewed_rows': len(rows),
            'correct': sum(r['correct'] for r in rows),
            'newly_read': sum(r['newly_read'] for r in rows),
            'wrong_known': sum(r['known'] and not r['correct'] for r in rows),
            'remaining_unknown': sum(not r['known'] for r in rows),
            'protected_field_violations': violations, 'rows': rows}


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('raw', type=Path)
    p.add_argument('--gold', type=Path, default=Path('evidence/MENU_QUANTITY_REVIEW.json'))
    p.add_argument('--output', type=Path, default=Path('evidence/raw/missing-quantity-evaluation.json'))
    a = p.parse_args()
    result = evaluate(json.loads(a.raw.read_text('utf-8')), json.loads(a.gold.read_text('utf-8')))
    a.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({k: v for k, v in result.items() if k != 'rows'}, ensure_ascii=False, indent=2))
