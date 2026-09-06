"""Gold is read ONLY here, after recognizers and policies have finished."""
import json
from menu_probe import ROOT, RAW, load
from menu_select import select, vote, centers, key
from evaluate_engines import matches

if __name__ == '__main__':
    data = load(RAW/'menu-pc-probe.json')['rows']
    extra = load(RAW/'menu-pc-extra.json')['rows']
    for row, add in zip(data, extra):
        assert (row['file'],row['quad']) == (add['file'],add['quad'])
        row['variants'].update(add['variants'])
    gold = load(ROOT/'evidence/document-scans-sample.json')
    third = next(r for r in load(ROOT/'evidence/expected.json') if r['file']=='03.jpg')
    gold03 = [{'file':'03.jpg', 'names':[n for n,p,q in third['items'] if p>0]}]
    policies = {'old': lambda r:r['old'], 'strict':lambda r:select(r)[0],
                'vote_numbers':vote, 'vote_no_numbers':lambda r:vote(r,False), 'centers':centers}
    policies.update({k:lambda r,k=k:r['variants'][k]['text'] for k in data[0]['variants']})
    output = {'limitation':'Exploratory reused fixed sample; name coverage only; no held-out accuracy claim.', 'policies':{}}
    for mode, policy in policies.items():
        selected = [(r,policy(r)) for r in data]
        stats = {}
        for label, refs in [('fixed34',gold),('03positive10',gold03)]:
            before=[]; after=[]; differences=[]
            for ref in refs:
                rows = [(r,t) for r,t in selected if r['file']==ref['file']]
                old = matches(ref['names'],[r['old'] for r,t in rows])
                new = matches(ref['names'],[t for r,t in rows])
                before += old; after += new
                differences += [{'file':ref['file'],'name':n,'before':a,'after':b}
                                for n,a,b in zip(ref['names'],old,new) if a!=b]
            stats[label] = {'before':sum(before),'after':sum(after),'count':len(before),
                            'gains':sum(not a and b for a,b in zip(before,after)),
                            'losses':sum(a and not b for a,b in zip(before,after)), 'differences':differences}
        stats['changes'] = [{'file':r['file'],'old':r['old'],'new':t} for r,t in selected if key(r['old'])!=key(t)]
        output['policies'][mode] = stats
        print(mode, {k:(v['after'],v['gains'],v['losses']) for k,v in stats.items() if isinstance(v,dict)})
    (RAW/'menu-evaluation.json').write_text(json.dumps(output,ensure_ascii=False,indent=2),encoding='utf-8')
