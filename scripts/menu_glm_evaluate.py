"""Reference names are used only for post-inference evaluation, never recognition."""
import collections
import json
import re
import statistics
from pathlib import Path
from menu_probe import ROOT,RAW,load
from evaluate_engines import norm

def coverage(names,text):
    # Permissive document-wide coverage; a duplicate name needs another occurrence.
    text=norm(re.sub('<[^>]*>',' ',text)); result=[]
    for name in names:
        at=text.find(norm(name)); result.append(at>=0)
        if at>=0: text=text[:at]+text[at+len(norm(name)):]
    return result

if __name__ == '__main__':
    gold={r['file']:r['names'] for r in load(ROOT/'evidence/document-scans-sample.json')}
    third=next(r for r in load(ROOT/'evidence/expected.json') if r['file']=='03.jpg')
    gold['03.jpg']=[n for n,p,q in third['items'] if p>0]
    out={'metric':'Permissive full name document coverage, whitespace/punctuation ignored; repeated names need repeated occurrences. Not row/price accuracy.','runs':{}}
    for file in sorted(RAW.glob('menu-glm*-results.json')):
        data=load(file); details=[]
        for row in data['rows']:
            text=row['response']['choices'][0]['message']['content']
            found=coverage(gold[row['file']],text)
            details.append({'file':row['file'],'correct':sum(found),'count':len(found),'seconds':row['seconds'],
                            'names':[{'name':n,'found':f} for n,f in zip(gold[row['file']],found)]})
        summary={'correct':sum(r['correct'] for r in details),'count':sum(r['count'] for r in details),
                 'medianSeconds':statistics.median(r['seconds'] for r in details),'details':details}
        out['runs'][file.name]=summary
        print(file.name,summary['correct'],summary['count'],round(summary['medianSeconds'],3))
    (RAW/'menu-glm-evaluation.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf8')
