"""Read-only name-stream CER/WER and multiset exact rows; no gold-driven correction."""
import argparse
from collections import Counter
import json
from pathlib import Path
import sys
import unicodedata as ud
from evaluate_engines import norm

def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))

def distance(a, b):
    previous = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        row = [i]
        for j, y in enumerate(b, 1):
            row.append(min(row[-1] + 1, previous[j] + 1,
                           previous[j - 1] + (x != y)))
        previous = row
    return previous[-1]

def names(items):
    return [' '.join(ud.normalize('NFC', i['name']).split()) for i in items]

def rowkey(i):
    return norm(i['name']), i.get('count', i.get('quantity')), i.get('printedTotal', i.get('amount'))

def score(gold, actual, include_zero):
    actual = [i for i in actual if not i.get('includedDiscount', False)
              and i.get('baseTotal', i['unit'] * i['count']) >= (0 if include_zero else 1)]
    a, b = names(gold), names(actual)
    result = {'gold_rows': len(a), 'output_rows': len(b)}
    for label, convert in [('cer', lambda s: s), ('compact_cer', norm),
                           ('jamo_cer', lambda s: ud.normalize('NFD', norm(s)))]:
        ref, hyp = '\n'.join(map(convert, a)), '\n'.join(map(convert, b))
        result[label + '_edits'] = distance(ref, hyp)
        result[label + '_units'] = len(ref)
    ref, hyp = ' '.join(a).split(), ' '.join(b).split()
    result.update(wer_edits=distance(ref, hyp), wer_units=len(ref))
    for label, ref, hyp in [('exact_names', a, b),
                            ('compact_names', list(map(norm, a)), list(map(norm, b))),
                            ('exact_rows', list(map(rowkey, gold)), list(map(rowkey, actual)))]:
        result[label] = sum((Counter(ref) & Counter(hyp)).values())
    return result

def evaluate(input_path, gold_path, include_zero):
    raw, gold = read(input_path), read(gold_path)
    by = {r['file']: r for r in raw}
    assert len(by) == len(raw), 'Duplicate input filenames'
    detail = {}
    for g in gold['receipts']:
        if 'items' not in g:
            continue
        r = by[g['file']]
        assert 'error' not in r, r.get('error')
        detail[g['file']] = score(g['items'], r['items'], include_zero)
    assert detail, 'No item-name gold supplied'
    total = {k: sum(d[k] for d in detail.values()) for k in next(iter(detail.values()))}
    for metric in ('cer', 'compact_cer', 'jamo_cer', 'wer'):
        total[metric] = total.get(metric + '_edits', 0) / total[metric + '_units']
    return {'input': str(input_path), 'gold': str(gold_path), 'include_zero': include_zero,
            'input_receipts': len(raw), 'evaluated_receipts': len(detail),
            'metrics': total, 'receipts': detail}

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--input', required=True)
    p.add_argument('--gold', required=True)
    p.add_argument('--include-zero', action='store_true')
    args = p.parse_args()
    sys.stdout.reconfigure(encoding='utf-8')
    print(json.dumps(evaluate(args.input, args.gold, args.include_zero), ensure_ascii=False, indent=2))
