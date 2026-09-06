"""Automatic name quads from existing OCR. Reference strings never enter this module."""
import json,re
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
RAW=ROOT/'evidence/raw'
load=lambda p:json.loads(p.read_text(encoding='utf-8-sig'))
key=lambda s:re.sub(r'[^\w]','',s,flags=re.UNICODE).replace('_','')

def groups(lines):
    slopes=[(q[1][1]-q[0][1])/(q[1][0]-q[0][0]) for l in lines
            if (q:=l['quad'])[1][0]-q[0][0]>80]
    slope=sorted(slopes)[len(slopes)//2] if slopes else 0
    def box(l):
        q=np.array(l['quad']);a=q.min(0);b=q.max(0)
        return (a+b)/2,b[1]-a[1]
    def y(l):
        p,_=box(l);return p[1]-p[0]*slope
    out=[]
    for l in sorted(lines,key=y):
        if not out or abs(y(l)-y(out[-1][0]))>min(box(l)[1],box(out[-1][0])[1])*.55:out.append([])
        out[-1].append(l)
    return [sorted(g,key=lambda l:box(l)[0][0]) for g in out]

def combined(parts):
    q=np.array(parts[0]['quad'],dtype=float)
    if len(parts)==1:return q.tolist()
    slope=(q[1,1]-q[0,1])/max(1,q[1,0]-q[0,0]);p=np.concatenate([l['quad'] for l in parts])
    x=p[:,0];y=p[:,1]-slope*x;l,r=x.min(),x.max();t,b=y.min(),y.max()
    return [[l,t+slope*l],[r,t+slope*r],[r,b+slope*r],[l,b+slope*l]]

def cases():
    # All 63+11 images; selection uses app output, never annotated menu names.
    for capture,baseline in [('document-scans-paddle-v6-retry.json','menus-app63.json'),
                             ('structure-v6-before11.json','menus-app11.json')]:
        receipts={r['file']:r for r in load(RAW/baseline)}
        for raw in load(RAW/capture):
            r=receipts[raw['file']];gg=groups(raw['lines']);used=set()
            for i,item in enumerate(r['items']):
                if item.get('baseTotal',0)<0 or item.get('includedDiscount'):continue
                aliases={key(item.get(k,'')) for k in ['name','originalName']}-{''}
                found={}
                for name in aliases:
                    for j,g in enumerate(gg):
                        parts=[l for l in g if (s:=key(l['text'])) and re.search('[가-힣A-Za-z]',s) and s in name]
                        if parts and ''.join(key(l['text']) for l in parts)==name:found[j]=parts
                if len(found)!=1:continue
                j,parts=next(iter(found.items()))
                if j in used:continue
                used.add(j)
                path=(ROOT/'app/src/androidTest/assets/receipts'/r['file'] if len(r['file'])==6
                      else Path('C:/Users/surromind/Downloads/Document scans')/r['file'])
                yield dict(file=r['file'],index=i,old=item['name'],source=str(path),quad=combined(parts))
