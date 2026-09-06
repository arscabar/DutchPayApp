"""Conservative complete-line selection from stored direct-warp outputs; no labels."""
import json,re
from region_data import RAW,load
from region_cross import normalized

def choose(row):
    old=normalized(row['old'])[0];views=row['views'];base=views['base']
    candidates=[views[k] for k in ['linear','cubic','lanczos']]
    texts=[normalized(v['text'])[0] for v in candidates]
    if normalized(base['text'])[0]!=old or len(set(texts))!=1:return row['old']
    new=texts[0]
    if len(old)!=len(new) or base['score']>=.95:return row['old']
    if min(v['score'] for v in candidates)<max(.97,base['score']+.05):return row['old']
    if any(a!=b and not ('가'<=a<='힣' and '가'<=b<='힣') for a,b in zip(old,new)):return row['old']
    out=list(row['old']);positions=normalized(row['old'])[1]
    for i,c in enumerate(new):out[positions[i]]=c
    return ''.join(out)

data=load(RAW/'region-warp.json')
for row in data['rows']:
    row['selected']=choose(row);row['edits']=[]
    if row['selected']!=row['old']:print(row['file'],row['old'],'=>',row['selected'])
(RAW/'region-warp-selected.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
