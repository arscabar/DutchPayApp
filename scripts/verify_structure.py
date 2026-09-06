"""Verify local v0.4 artifacts and unchanged reference inputs."""
import hashlib,json,statistics,zipfile
from pathlib import Path
from evaluate_steps import ROOT,read

def digest(path):return hashlib.file_digest(path.open('rb'),'sha256').hexdigest()

if __name__=='__main__':
    repo=ROOT.parent
    originals=Path('C:/Users/surromind/Downloads/Document scans')
    manifest=read('document-scans-manifest.json')
    assert len(manifest)==63
    assert all(digest(originals/r['file'])==r['sha256'] for r in manifest)
    assert digest(ROOT/'expected.json')=='afc90e1791b21eda1fc36ee5c36a8ca6901109c52cad2cad22d9ba8132419764'
    old=read('steps-apk.json')
    assert digest(repo/'dist/DutchPay-OCR-v0.3-debug.apk')==old['sha256']
    source=repo/'app/build/outputs/apk/debug/app-debug.apk'
    apk=repo/'dist/DutchPay-OCR-v0.4-debug.apk'
    assert digest(apk)==digest(source)
    models={}
    with zipfile.ZipFile(apk) as z:
        assert not any(n.lower().endswith(('.jpg','.jpeg')) for n in z.namelist())
        for name in old['models']:
            models[name]=hashlib.sha256(z.read('assets/models/'+name)).hexdigest()
            assert models[name]==old['models'][name]
    data=read('raw/structure-app63.json');legacy=read('raw/structure-app11.json')
    assert len(data)==63 and len(legacy)==11
    assert all('error' not in r for r in data+legacy)
    r=next(r for r in legacy if r['file']=='03.jpg')
    gold=read('STRUCTURE_GOLD.json')['receipts'][0]
    numeric=lambda items:[(i['unit'],i['count'],i['printedTotal']) for i in items if i['unit']>0]
    assert numeric(r['items'])==numeric(gold['items'])
    assert len(r['items'])==20 and sum(i['baseTotal'] for i in r['items'])==77300
    assert r['total']==77300
    ms=[r['milliseconds'] for r in data]
    report={'version':'0.4','processed':74,'errors':0,'originals_sha256_verified':63,
        'frozen_legacy_gold_preserved':True,'v03_apk_preserved':True,
        'source_and_distributed_apk_equal':True,'bytes':apk.stat().st_size,'sha256':digest(apk),
        'models':models,'receipt_images_in_apk':False,'option_positive_numeric_rows':10,
        'option_total_rows':20,'option_total':77300,
        'scan63_milliseconds':{'total':sum(ms),'median':statistics.median(ms),'min':min(ms),'max':max(ms)}}
    (ROOT/'structure-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps(report,ensure_ascii=False,indent=2))
