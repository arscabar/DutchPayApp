"""Post-hoc candidate ceiling ONLY. Reference labels are not an inference input."""
import json
from region_data import ROOT,RAW,load
from evaluate_menu_fields import evaluate
from evaluate_menu_align import selected
from evaluate_names import norm

result={}
for file in ['region-warp.json','region-cross-android.json']:
    rows={(r['file'],r['index']):r for r in load(RAW/file)['rows']};scores={}
    for label,base,gold,kind in [('fixed34','menus-app63.json','document-scans-amounts.json','positive'),
        ('original03','menus-app11.json','STRUCTURE_GOLD.json','nonnegative'),
        ('legacy17','menus-app11.json','MENU_LEGACY_GOLD.json','positive'),
        ('additional20','menus-app63.json','MENU_ADDITIONAL_GOLD.json','positive')]:
        baseline={r['file']:r for r in load(RAW/base)}
        score=evaluate(RAW/base,ROOT/'evidence'/gold,kind=kind);hits=[];observed=0
        for item in score['rows']:
            if not item.get('output_row'):continue
            indices=[i for i,v in enumerate(baseline[item['file']]['items']) if selected(v,kind)]
            row=rows.get((item['file'],indices[item['output_row']-1]))
            if row is None:continue
            observed+=1
            if 'name' not in item['incorrect']:continue
            texts=[v['text'] for v in row['views'].values()] if 'views' in row else [row['ml'],''.join(t['text'] for t in row['tokens'])]
            correct=[text for text in texts if norm(text)==norm(item['expected']['name'])]
            if correct:hits.append(dict(file=item['file'],old=row['old'],observedCandidates=correct))
        scores[label]=dict(baseline=score['fields']['name'],observedRows=observed,
            oracleOnlyAdditionalCorrect=len(hits),candidateHits=hits)
    result[file]=scores
(RAW/'region-oracle-evaluation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))
