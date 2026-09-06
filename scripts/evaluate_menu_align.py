"""Evaluation-only monotonic name alignment; never used by the Android app."""
from evaluate_names import distance, norm

def align(gold, output):
    a = [norm(i.get('name') or '') for i in gold]
    b = [norm(i['name']) for i in output]
    cost = [[0.] * (len(b) + 1) for _ in range(len(a) + 1)]
    step = {}
    for i in range(1, len(a) + 1):
        cost[i][0] = i
        step[i, 0] = 'missing'
    for j in range(1, len(b) + 1):
        cost[0][j] = j
        step[0, j] = 'extra'
    for i, x in enumerate(a, 1):
        for j, y in enumerate(b, 1):
            mismatch = distance(x, y) / max(1, len(x), len(y))
            cost[i][j], step[i, j] = min([
                (cost[i-1][j-1] + mismatch, 'pair'),
                (cost[i-1][j] + 1, 'missing'), (cost[i][j-1] + 1, 'extra')], key=lambda v: v[0])
    pairs, extra = [], []
    i, j = len(a), len(b)
    while i or j:
        move = step[i, j]
        if move == 'pair':
            i -= 1; j -= 1; pairs.append((i, j))
        elif move == 'missing':
            i -= 1; pairs.append((i, None))
        else:
            j -= 1; extra.append(j)
    return list(reversed(pairs)), list(reversed(extra))

def expected(i):
    return {'name': i.get('name'), 'unit': i.get('unit', i.get('unit_price')),
            'count': i.get('count', i.get('quantity')), 'printedTotal': i.get('printedTotal', i.get('amount'))}

def actual(i):
    return {**expected(i), 'unit': None if i.get('amountBased', False) else i.get('unit'),
            'count': i.get('count') if i.get('quantityKnown', True) else None}

def selected(i, kind):
    if i.get('includedDiscount', False):
        return False
    if kind == 'removed':
        return i.get('count', 0) < 0
    amount = i.get('baseTotal', i['unit'] * i['count'])
    return {'positive': amount > 0, 'zero': amount == 0, 'nonnegative': amount >= 0}[kind]

def joined_cells(gold, output):
    a, b = [norm(i['name']) for i in gold], [norm(i['name']) for i in output]
    suspects = []
    for n in range(len(a)-1):
        for j, text in enumerate(b):
            if a[n] and a[n+1] and a[n]+a[n+1] in text:
                suspects.append({'kind':'merged_names', 'gold_rows':[n+1,n+2], 'output_rows':[j+1]})
    for n, text in enumerate(a):
        for j in range(len(b)-1):
            if b[j] and b[j+1] and b[j]+b[j+1] == text:
                suspects.append({'kind':'split_name', 'gold_rows':[n+1], 'output_rows':[j+1,j+2]})
    return suspects
