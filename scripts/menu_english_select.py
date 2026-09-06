"""Only replace observed Latin image spans; all other source characters stay intact."""
import collections
import re
TOKEN=re.compile(r'[A-Za-z0-9][A-Za-z0-9 .%/+_()−-]*')
compact=lambda s:re.sub(r'\s','',s)
punct=lambda s:re.sub(r'[A-Za-z0-9\s]','',s)
body=lambda s:compact(TOKEN.sub(lambda m:'' if re.search('[A-Za-z]',m.group()) else m.group(),s))

def number_safe(original,next):
    a,b=compact(original),compact(next)
    if len(a)!=len(b):return re.sub(r'\D','',a)==re.sub(r'\D','',b)
    allowed={frozenset(pair) for pair in ['1l','1I','0O','0o','7l']}
    for x,y in zip(a,b):
        if x!=y and (x.isdigit() or y.isdigit()) and frozenset((x,y)) not in allowed:return False
    return True

def choose(original,observed):
    candidates=collections.defaultdict(list)
    bounds=set()
    for v in observed['variants'].values():
        area=tuple(v['x'])
        if area in bounds:continue
        bounds.add(area)
        text=v['text'].strip()
        if v['score']>=.95 and re.search('[A-Za-z]',text) and TOKEN.fullmatch(text):
            candidates[compact(text)].append(v)
    accepted=[g for g in candidates.values() if len(g)>=2]
    if len(accepted)!=1:return original
    text=max(accepted[0],key=lambda v:v['score'])['text'].strip()
    if punct(original)!=punct(text) or not number_safe(original,text):return original
    a=re.match(r'\d+',original);b=re.match(r'\d+',text)
    if (a.group() if a else '')!=(b.group() if b else ''):return original
    return text

def apply(original,probes):
    changes=[]; tokens=list(TOKEN.finditer(original))
    for probe in probes:
        if body(original)!=body(probe['ctcText']):continue
        source=list(TOKEN.finditer(probe['ctcText']))
        index=next((i for i,m in enumerate(source) if list(m.span())==probe['range']),None)
        if index is None or len(source)!=len(tokens):continue
        token=tokens[index];text=choose(token.group(),probe)
        if compact(text)!=compact(token.group()):changes.append((token.start(),token.end(),text))
    for a,b,text in sorted(set(changes),reverse=True):original=original[:a]+text+original[b:]
    return original
