"""Read overlapping observed CTC regions; no dictionaries or text generation."""
import math
import numpy as np
from menu_ctc_spans import infer,aligned

def read(session,chars,crop):
    values,full=infer(session,crop);tokens=[t for t in aligned(values,chars) if not t['text'].isspace()]
    for token in tokens:
        i=chars.index(token['text'])
        token['score']=float(values[token['first'],i])
    return tokens,full/len(values)

def windows(session,chars,crop,tokens,unit):
    h,w=crop.shape[:2];n=len(tokens);centers=[(t['first']+t['last']+1)/2*unit for t in tokens]
    edges=[0]+[(a+b)/2 for a,b in zip(centers,centers[1:])]+[w]
    output=[];seen=set()
    for size in [3,5,7]:
        if size>=n:continue
        for start in range(0,n-size+1,max(1,size//2)):
            end=start+size
            # Neighbor CTC center midpoints locate crop edges, independent of glyph values.
            l=max(0,math.floor(edges[start]));r=min(w,math.ceil(edges[end]))
            if (l,r) in seen or r-l<8:continue
            seen.add((l,r));new,_=read(session,chars,crop[:,l:r])
            text=''.join(t['text'] for t in new)
            output.append(dict(start=start,end=end,left=l,right=r,text=text,
                               scores=[t['score'] for t in new]))
    return output

def choose(tokens,observed):
    from collections import defaultdict
    votes=defaultdict(list);n=len(tokens)
    for view in observed:
        a,b=view['start'],view['end'];text=view['text']
        if len(text)!=b-a:continue
        # Crop edge letters must agree with the whole-line reading.
        if a and text[0]!=tokens[a]['text']:continue
        if b<n and text[-1]!=tokens[b-1]['text']:continue
        for j,c in enumerate(text):
            i=a+j
            if i and j==0 or i<n-1 and j==len(text)-1:continue
            if '가'<=c<='힣' and view['scores'][j]>=.97:votes[i].append((c,view['scores'][j]))
    changed={}
    for i,vv in votes.items():
        old=tokens[i]
        if not ('가'<=old['text']<='힣') or old['score']>=.95:continue
        counts={c:sum(x==c for x,_ in vv) for c,_ in vv}
        winner=[c for c,count in counts.items() if count>=2]
        if len(winner)==1 and winner[0]!=old['text']:
            changed[i]=winner[0]
    return ''.join(changed.get(i,t['text']) for i,t in enumerate(tokens)),changed
