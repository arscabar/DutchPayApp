"""Require observed alternative to strengthen after removing top/bottom margins."""
from region_cross import normalized

def single_difference(original,other):
    if len(original)!=len(other):return False
    differences=[(a,b) for a,b in zip(original,other) if a!=b]
    return len(differences)==1 and all('가'<=c<='힣' for c in differences[0])

def aligned(tokens):
    tokens=[t for t in tokens if not t['text'].isspace()]
    text,indices=normalized(''.join(t['text'] for t in tokens))
    return text,[tokens[i] for i in indices]

def choose(old,base,center,ml):
    text,positions=normalized(old);other,_=normalized(ml)
    a,aa=aligned(base);b,bb=aligned(center)
    if len({len(text),len(other),len(a),len(b)})!=1 or not single_difference(text,other):return old,[]
    out=list(old);edits=[]
    for i,(original,candidate) in enumerate(zip(text,other)):
        if original==candidate or not all('가'<=c<='힣' for c in [original,candidate]):continue
        if a[i]!=original or b[i] not in [original,candidate]:continue
        first=aa[i];second=bb[i]
        top=first['alternatives'][:2] # blank in second place vetoes a weaker letter
        p=next((t['score'] for t in top if t['text']==candidate),0)
        scores={t['text']:t['score'] for t in second['alternatives']}
        # A crop that reinforces the existing glyph vetoes an uncertain base crop.
        if first['score']>=.95 or p<.08:continue
        q=scores.get(candidate,0);previous=scores.get(original,0)
        if q<=p or previous>=first['score']:continue
        out[positions[i]]=candidate
        edits.append(dict(index=i,old=original,new=candidate,baseOld=first['score'],
                          baseNew=p,centerOld=previous,centerNew=q))
    return ''.join(out),edits

if __name__=='__main__':
    import argparse,json
    from region_data import RAW,load
    ap=argparse.ArgumentParser();ap.add_argument('--android',action='store_true');args=ap.parse_args()
    variants={r['png']:r for r in load(RAW/'region-variant-trace.json')['rows']}
    actual={r['png']:r for r in load(RAW/'region-probe/region-center.json')} if args.android else {}
    output=[]
    for row in load(RAW/'region-cross-android.json')['rows']:
        center=actual[row['png']]['tokens'] if args.android else variants[row['png']]['views'][1]['tokens']
        selected,edits=choose(row['old'],row['tokens'],center,row['ml'])
        output.append(dict(row,centerTokens=center,selected=selected,edits=edits))
        if edits:print(row['file'],row['old'],'=>',selected,edits)
    name='region-direction-android.json' if args.android else 'region-direction-mixed.json'
    method='Actual Android base/ML and cached PNG center80 CTC' if args.android else 'Actual Android base and ML; PC center80 CTC on actual PNG'
    (RAW/name).write_text(json.dumps(dict(method=method,rows=output),
        ensure_ascii=False,indent=2),encoding='utf-8')
