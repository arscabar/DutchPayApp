"""Read-only transitions and numeric-change inventory; alignment uses names only."""
from evaluate_menu_align import align
from evaluate_menu_fields import evaluate

MONEY = ('unit','count','printedTotal','baseTotal','amountBased','includedDiscount','quantityKnown')

def money_value(item, key):
    return item.get(key, True if key == 'quantityKnown' else None)

def transitions(before, after):
    old = {(r['file'],r['gold_row']):r for r in before['rows'] if 'gold_row' in r}
    result = dict(correct_to_wrong_fields=[], wrong_to_correct_fields=[],
                  complete_to_incomplete=[], incomplete_to_complete=[])
    for row in after['rows']:
        if 'gold_row' not in row:
            continue
        prior = old[row['file'],row['gold_row']]
        known = {k for k,v in row['expected'].items() if v is not None}
        was, now = set(prior['incorrect']), set(row['incorrect'])
        detail = {'file':row['file'],'gold_row':row['gold_row'],
                  'expected':row['expected'],'before':prior['actual'],'after':row['actual']}
        for key, changed in [('correct_to_wrong_fields',now-was),('wrong_to_correct_fields',was-now)]:
            if changed & known:
                result[key].append({**detail,'fields':sorted(changed & known)})
        if not was and now:
            result['complete_to_incomplete'].append(detail)
        if was and not now:
            result['incomplete_to_complete'].append(detail)
    return result

def cohort(before, after, gold, section='items', kind='positive'):
    a, b = [evaluate(p,gold,section,kind) for p in (before,after)]
    return {'before':a,'after':b,'transitions':transitions(a,b)}

def financial_changes(before, after):
    old = {r['file']:r for r in before}
    changes = []
    compact = lambda i: {k:money_value(i,k) for k in ('name',)+MONEY}
    for row in after:
        previous = old[row['file']]
        if previous.get('total') != row.get('total'):
            changes.append({'file':row['file'],'field':'receipt_total',
                'before':previous.get('total'),'after':row.get('total')})
        a,b = previous['items'],row['items']
        pairs, extras = align(a,b)
        for n,j in pairs:
            fields = list(MONEY) if j is None else [k for k in MONEY if money_value(a[n],k)!=money_value(b[j],k)]
            if fields:
                changes.append({'file':row['file'],'before_row':n+1,'after_row':None if j is None else j+1,
                    'before':compact(a[n]),'after':None if j is None else compact(b[j]),'fields':fields})
        for j in extras:
            changes.append({'file':row['file'],'before_row':None,'after_row':j+1,
                            'before':None,'after':compact(b[j]),'fields':list(MONEY)})
    return changes
