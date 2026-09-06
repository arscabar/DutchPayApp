"""Validate real ML characters against aligned Paddle emissions; no word dictionary."""
import re

def normalized(text):
    positions=[i for i,c in enumerate(text) if not c.isspace()]
    while positions and text[positions[0]] in '▶►→':positions.pop(0)
    if len(positions)>1 and text[positions[0]]=='-' and '가'<=text[positions[1]]<='힣':positions.pop(0)
    return ''.join(text[i] for i in positions),positions

def choose(old,tokens,ml,min_alt=.08,min_ratio=.08):
    tokens=[t for t in tokens if not t['text'].isspace()]
    base=''.join(t['text'] for t in tokens);a,pos=normalized(old);b,_=normalized(ml)
    c,indices=normalized(base);tokens=[tokens[i] for i in indices]
    if len(a)!=len(b) or len(a)!=len(c):return old,[]
    out=list(old);accepted=[]
    for i,(original,other,fresh) in enumerate(zip(a,b,c)):
        if original==other or not all('가'<=v<='힣' for v in [original,other,fresh]):continue
        token=tokens[i];reason=None
        if fresh==other and token['score']>=.90:reason='both engines read same Hangul at same position'
        elif fresh==original and token['score']<.95:
            top=[v for v in token['alternatives'] if v['text']]
            candidate=next((v for v in top[:2] if v['text']==other),None)
            if candidate and candidate['score']>=min_alt and candidate['score']>=token['score']*min_ratio:
                reason='ML observed Hangul is Paddle nonblank top-2; original glyph uncertain'
        if reason:
            out[pos[i]]=other;accepted.append(dict(index=i,old=original,new=other,reason=reason,score=token['score']))
    return ''.join(out),accepted
