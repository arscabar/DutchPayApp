"""Predeclared candidate policies. No receipt gold, dictionaries, or price inference."""
import collections
import re

FAMILIES = ['sdk', 'quad', 'unsharp', 'clahe', 'aspect0.8', 'aspect1.2']
key = lambda s: re.sub(r'[^\w]', '', s)
numbers = lambda s: re.findall(r'[-−]?\d[\d,]*', s)


def select(row, numeric_guard=True, require_old_base=False):
    old = row['old']; variants = row['variants']
    refs = [variants[m]['score'] for m in ['sdk', 'quad'] if key(variants[m]['text']) == key(old)]
    if require_old_base and not refs:
        return old, 'no matching base'
    groups = collections.defaultdict(list)
    for mode in FAMILIES:
        groups[key(variants[mode]['text'])].append(variants[mode])
    eligible = []
    for group in groups.values():
        best = max(group, key=lambda x: x['score']); text = best['text']
        if key(text) == key(old) or not re.search('[가-힣]', text): continue
        if not .7*len(key(old)) <= len(key(text)) <= 1.3*len(key(old))+2: continue
        if numeric_guard and numbers(old) != numbers(text): continue
        mean = sum(x['score'] for x in group)/len(group)
        if refs:
            accepted = len(group) >= 2 and mean >= .90 and best['score'] >= .95 and best['score'] >= max(refs)+.03
        else:
            accepted = len(group) >= 3 and mean >= .93 and best['score'] >= .97
        if accepted: eligible.append((len(group), mean, text))
    if not eligible: return old, 'retained'
    eligible.sort(reverse=True)
    return eligible[0][2], 'consensus with reference' if refs else 'consensus without matching reference'


def vote(row, numeric_guard=True):
    groups = collections.defaultdict(list)
    for mode in FAMILIES:
        v = row['variants'][mode]; groups[key(v['text'])].append(v)
    ranked = sorted(groups.values(), key=len, reverse=True)
    g = ranked[0]; best = max(g, key=lambda x: x['score']); old = row['old']
    if len(g) < 3 or (len(ranked) > 1 and len(g) == len(ranked[1])): return old
    if sum(x['score'] for x in g)/len(g) < .80: return old
    if max(v['score'] for v in row['variants'].values())-best['score'] > .10: return old
    if numeric_guard and numbers(old) != numbers(best['text']): return old
    if not re.search('[가-힣]', best['text']): return old
    return best['text']


def centers(row):
    """Exploratory identical central crops; thresholds need a fresh holdout."""
    old = row['old']; v = row['variants']
    a, b = v['center60'], v['center70']
    if key(a['text']) != key(b['text']) or numbers(a['text']) != numbers(old): return old
    if min(a['score'], b['score']) < .9 or max(a['score'], b['score']) < .97: return old
    if max(v['sdk']['score'], v['quad']['score']) >= .9: return old
    if not .7*len(key(old)) <= len(key(a['text'])) <= 1.3*len(key(old))+2: return old
    return a['text']
