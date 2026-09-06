"""Map actual Android CTC/ML trace to frozen final names, without labels."""
import json
from region_data import RAW,load
from region_cross import normalized,choose

baseline={r['file']:r for f in ['menus-app63.json','menus-app11.json'] for r in load(RAW/f)}
trace=load(RAW/'region-probe/region-probe.json');rows=[];unmatched=[]
for receipt in trace:
    used=set();items=baseline[receipt['file']]['items']
    for name in receipt['names']:
        old=normalized(name['old'])[0]
        matches=[i for i,item in enumerate(items) if i not in used and old in {
            normalized(item['name'])[0],normalized(item.get('originalName',''))[0]}]
        if not matches:
            unmatched.append(dict(file=receipt['file'],old=name['old']));continue
        index=matches[0];used.add(index);final=items[index]['name']
        selected,edits=choose(final,name['tokens'],name['ml'])
        rows.append(dict(file=receipt['file'],index=index,old=final,preReview=name['old'],
            tokens=name['tokens'],ml=name['ml'],selected=selected,edits=edits,
            png=name['png'],width=name['width'],height=name['height']))
out=dict(source='region-probe/region-probe.json',files=len(trace),rows=rows,unmatched=unmatched)
(RAW/'region-cross-android.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf-8')
print('files',len(trace),'matched',len(rows),'unmatched',unmatched)
for r in rows:
    if r['old']!=r['selected']:print(r['file'],r['old'],'=>',r['selected'])
