"""Post-inference full-menu coverage evaluation; no reference input to the models."""
import json
from menu_probe import ROOT,RAW,load
from menu_english_select import apply
from evaluate_engines import matches

if __name__=='__main__':
    probes=load(RAW/'menu-english-probe.json')['rows']
    actual=load(RAW/'names-app63.json');gold=load(ROOT/'evidence/document-scans-sample.json')
    changes=[];before=after=gains=loss=0
    for row in actual:
        names=[i['name'] for i in row['items']]
        new=[apply(n,[p for p in probes if p['file']==row['file']]) for n in names]
        changes.extend({'file':row['file'],'old':n,'new':t} for n,t in zip(names,new) if n!=t)
        ref=next((g for g in gold if g['file']==row['file']),None)
        if ref:
            a=matches(ref['names'],names);b=matches(ref['names'],new)
            before+=sum(a);after+=sum(b)
            gains+=sum(not x and y for x,y in zip(a,b));loss+=sum(x and not y for x,y in zip(a,b))
    result={'before':before,'after':after,'count':34,'gains':gains,'losses':loss,'changes':changes,
            'limitation':'PC CTC-cropped spans; 12 mixed spans probed. Other names remain unchanged; no holdout claim.'}
    (RAW/'menu-english-selection.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
    print(before,after,gains,loss)
