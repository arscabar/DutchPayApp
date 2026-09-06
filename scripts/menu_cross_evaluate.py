"""Post-inference evaluation; reference answers never enter the selector."""
import json
import argparse
from menu_probe import ROOT,RAW,load
from menu_cross_candidates import observations
from menu_cross_select import choose
from menu_select import key
from evaluate_engines import matches

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    for flag in ['android','insert','minimal']:parser.add_argument('--'+flag,action='store_true')
    args=parser.parse_args()
    rows=observations(args.android); gold=load(ROOT/'evidence/document-scans-sample.json')
    first=[dict(r,names=[n for n,p,q in r['items'] if p>0]) for r in load(ROOT/'evidence/expected.json')]
    refs={'fixed34':gold,'03positive10':[r for r in first if r['file']=='03.jpg'],
          'legacy17':[r for r in first if r['file']!='03.jpg']}
    out={'limitation':'Exploratory reused samples; untouched names retained. Not a held-out estimate.',
         'rowsWithML':sum(bool(r['ml']) for r in rows),'policies':{}}
    for multiple in [True,False]:
        for threshold in [.85,.9,.95]:
            for extra in [False,True]:
                tag=f'multiple{multiple}_score{threshold}_extra{extra}'
                selected=[choose(r,extra,threshold=threshold,multiple=multiple,
                                 allow_insert=args.insert,minimal=args.minimal)[0] for r in rows]
                result={}
                for label,cases in refs.items():
                    before=[];after=[];diff=[]
                    for case in cases:
                        relevant=[(r,t) for r,t in zip(rows,selected) if r['file']==case['file']]
                        a=matches(case['names'],[r['old'] for r,t in relevant]); b=matches(case['names'],[t for r,t in relevant])
                        before+=a;after+=b
                        diff.extend({'file':case['file'],'name':n,'before':x,'after':y}
                                    for n,x,y in zip(case['names'],a,b) if x!=y)
                    result[label]={'before':sum(before),'after':sum(after),'count':len(before),
                        'gains':sum(not a and b for a,b in zip(before,after)),
                        'losses':sum(a and not b for a,b in zip(before,after)),'differences':diff}
                result['changes']=[{'file':r['file'],'old':r['old'],'new':t} for r,t in zip(rows,selected) if key(t)!=key(r['old'])]
                out['policies'][tag]=result
                print(tag,{k:(v['after'],v['gains'],v['losses']) for k,v in result.items() if isinstance(v,dict)})
    label='menu-cross'+''.join('-'+f for f in ['android','insert','minimal'] if getattr(args,f))
    (RAW/(label+'-evaluation.json')).write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf8')
