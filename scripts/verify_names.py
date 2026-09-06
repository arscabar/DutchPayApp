"""Verify v0.5 artifacts, input preservation and financial regression boundaries."""
import hashlib,json,statistics,zipfile
from pathlib import Path
from verify_structure import digest
from evaluate_steps import ROOT,read

if __name__=='__main__':
    repo=ROOT.parent; previous=read('structure-verification.json')
    manifest=read('document-scans-manifest.json')
    assert len(manifest)==63
    assert all(digest(Path('C:/Users/surromind/Downloads/Document scans')/r['file'])==r['sha256'] for r in manifest)
    assert digest(ROOT/'expected.json')=='afc90e1791b21eda1fc36ee5c36a8ca6901109c52cad2cad22d9ba8132419764'
    assert digest(repo/'dist/DutchPay-OCR-v0.4-debug.apk')==previous['sha256']
    assert digest(repo/'dist/DutchPay-OCR-v0.3-debug.apk')==read('steps-apk.json')['sha256']
    apk=repo/'dist/DutchPay-OCR-v0.5-debug.apk'
    assert digest(apk)==digest(repo/'app/build/outputs/apk/debug/app-debug.apk')
    with zipfile.ZipFile(apk) as z:
        assert not any(n.lower().endswith(('.jpg','.jpeg')) for n in z.namelist())
        assert all(hashlib.sha256(z.read('assets/models/'+n)).hexdigest()==h for n,h in previous['models'].items())
    numeric=lambda rows:[{k:i[k] for k in ('unit','count','printedTotal','baseTotal','amountBased','includedDiscount')} for i in rows]
    scans=read('raw/names-app63.json'); legacy=read('raw/names-app11.json')
    for suffix,rows,count in [('63',scans,63),('11',legacy,11)]:
        old={r['file']:r for r in read('raw/structure-app'+suffix+'.json')}
        assert len(rows)==count and set(old)=={r['file'] for r in rows}
        for r in rows:
            b=old[r['file']];assert 'error' not in r
            assert r['originalRows']==b['originalRows'] and r.get('total')==b.get('total'),r['file']
            items=r['items']
            if r['file']=='Scan_20260729_191830.jpg':
                assert len(items)==len(b['items'])+1 and items[-1]['baseTotal']==-2400
                assert sum(i['baseTotal'] for i in items)==10000;items=items[:-1]
            assert numeric(items)==numeric(b['items']),r['file']
    ms=[r['milliseconds'] for r in scans]
    result=dict(version='0.5',processed=74,errors=0,originals_verified=63,
        original_ocr_rows_unchanged=74,original_totals_unchanged=74,
        prior_apks_preserved=True,models_unchanged=previous['models'],
        financial_change='07/29 explicit discount -2400 only',
        bytes=apk.stat().st_size,sha256=digest(apk),
        scan63_milliseconds=dict(total=sum(ms),median=statistics.median(ms),min=min(ms),max=max(ms)))
    (ROOT/'names-verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps(result,ensure_ascii=False,indent=2))
