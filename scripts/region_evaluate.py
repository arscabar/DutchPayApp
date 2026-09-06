"""Post-inference evaluation only: annotated text is never passed to recognition."""
import argparse,copy,json
from region_data import RAW,ROOT,load
from region_cross import choose
from evaluate_menu_fields import evaluate

ap=argparse.ArgumentParser();ap.add_argument('--input',default='region-cross-pc.json');ap.add_argument('--selected',action='store_true');args=ap.parse_args()
probe=load(RAW/args.input);results={}
for threshold in (['selected'] if args.selected else [.05,.08,.10]):
    base={f:load(RAW/f) for f in ['menus-app63.json','menus-app11.json']}
    output=copy.deepcopy(base);by={r['file']:r for rows in output.values() for r in rows};changes=[]
    for row in probe['rows']:
        selected,why=(row['selected'],row['edits']) if args.selected else choose(row['old'],row['tokens'],row['ml'],threshold,threshold)
        by[row['file']]['items'][row['index']]['name']=selected
        if selected!=row['old']:changes.append(dict(file=row['file'],old=row['old'],new=selected,evidence=why))
    paths={}
    for f,rows in output.items():
        paths[f]=RAW/f'{args.input[:-5]}-eval-{threshold}-{f}'
        paths[f].write_text(json.dumps(rows,ensure_ascii=False),encoding='utf-8')
    scores={}
    for label,file,gold,kind in [('fixed34','menus-app63.json','document-scans-amounts.json','positive'),
          ('original03','menus-app11.json','STRUCTURE_GOLD.json','nonnegative'),
          ('legacy17','menus-app11.json','MENU_LEGACY_GOLD.json','positive'),
          ('additional20','menus-app63.json','MENU_ADDITIONAL_GOLD.json','positive')]:
        before=evaluate(RAW/file,ROOT/'evidence'/gold,kind=kind);after=evaluate(paths[file],ROOT/'evidence'/gold,kind=kind)
        gains=losses=0
        for a,b in zip(before['rows'],after['rows']):
            if 'incorrect' not in a or 'incorrect' not in b:continue
            gains+= 'name' in a['incorrect'] and 'name' not in b['incorrect']
            losses+='name' not in a['incorrect'] and 'name' in b['incorrect']
        scores[label]=dict(before=before['fields']['name'],after=after['fields']['name'],gains=gains,losses=losses)
    results[str(threshold)]=dict(changes=changes,scores=scores)
out=RAW/args.input.replace('.json','-evaluation.json');out.write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(results,ensure_ascii=False,indent=2))
