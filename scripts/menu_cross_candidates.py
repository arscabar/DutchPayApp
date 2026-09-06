"""Join already observed model candidates, without reference names or prices."""
from menu_probe import ROOT,RAW,load
from menu_select import key

def candidates(file,old,pc,diagnostics):
    rows=[r for r in pc if r['file']==file]
    parts=[r for r in rows if key(r['old'])==key(old)]
    if not parts:
        parts=sorted([r for r in rows if key(r['old']) in key(old)],key=lambda r:min(p[0] for p in r['quad']))
        if ''.join(key(r['old']) for r in parts)!=key(old):parts=[]
    out=[]
    if parts:
        for mode in parts[0]['variants']:
            values=[r['variants'][mode] for r in parts]
            out.append({'mode':mode,'text':' '.join(v['text'] for v in values),
                        'score':min(v['score'] for v in values)})
    diag=next((d for d in diagnostics if key(d['old'])==key(old)),None)
    if diag:
        for mode in ['base','padded']:
            out.append({'mode':'android_'+mode,'text':diag[mode],'score':diag[mode+'Score']})
    return out

def observations(android=False):
    pc=load(RAW/'menu-pc-probe.json')['rows']; extra=load(RAW/'menu-pc-extra.json')['rows']
    for row, add in zip(pc,extra):
        assert (row['file'],row['quad'])==(add['file'],add['quad'])
        row['variants'].update(add['variants'])
    views={r['file']:r['names'] for r in load(RAW/'menu-view-probe.json')}
    native={r['file']:r['names'] for r in load(RAW/'menu-paddle-views.json')} if android else {}
    result=[]
    for receipt in load(RAW/'names-app63.json')+load(RAW/'names-app11.json'):
        for index,item in enumerate(receipt['items']):
            keys={key(item['name']),key(item.get('originalName',''))}
            view=next((v for v in views.get(receipt['file'],[]) if key(v['old']) in keys),None)
            row={'file':receipt['file'],'index':index,'old':item['name'],'ml':[],'paddle':[]}
            if view:
                row['ml']=view['views']
                row['paddle']=candidates(receipt['file'],view['old'],pc,receipt.get('nameDiagnostics',[]))
                if android:
                    source=next((v for v in native.get(receipt['file'],[]) if key(v['old'])==key(view['old'])),None)
                    row['paddle']=[] if source is None else [dict(v,mode=m) for m,v in zip(
                        ['android_base','center80','aspect0.8','aspect1.2','android_padded'],source['paddle'])]
                    if source:row['paddle'].append({'mode':'original','text':source['old'],'score':source['originalScore']})
            result.append(row)
    return result
