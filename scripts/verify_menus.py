"""Check release provenance and preserve the full before/after change inventory."""
import argparse,hashlib,json,statistics,zipfile
from pathlib import Path
from verify_structure import digest
from evaluate_steps import ROOT,read

def diagnostic_errors(value):
    if isinstance(value,list):return sum(diagnostic_errors(v) for v in value)
    if isinstance(value,dict):return int(bool(value.get('error')))+sum(diagnostic_errors(v) for v in value.values())
    return 0

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--baseline',default='names');p.add_argument('--current',default='menus')
    p.add_argument('--prior-version',default='0.5');p.add_argument('--version',default='0.6')
    a=p.parse_args();repo=ROOT.parent;previous=read(a.baseline+'-verification.json')
    manifest=read('document-scans-manifest.json')
    assert len(manifest)==63
    assert all(digest(Path('C:/Users/surromind/Downloads/Document scans')/r['file'])==r['sha256'] for r in manifest)
    original11=read('expected.json')
    assert len(original11)==11
    assert all(digest(Path('C:/Users/surromind/Downloads')/r['source'])==digest(
        repo/'app/src/androidTest/assets/receipts'/r['file']) for r in original11)
    assert digest(ROOT/'expected.json')=='afc90e1791b21eda1fc36ee5c36a8ca6901109c52cad2cad22d9ba8132419764'
    assert digest(repo/f'dist/DutchPay-OCR-v{a.prior_version}-debug.apk')==previous['sha256']
    apk=repo/f'dist/DutchPay-OCR-v{a.version}-debug.apk'
    apk_hash=digest(apk)
    assert apk_hash==digest(repo/'app/build/outputs/apk/debug/app-debug.apk')
    models=dict(previous.get('models_unchanged',previous.get('models_verified',{})))
    assert models
    models['english.onnx']='b5f833dfc5d0eb71da397b4efa06ebeee9b431b690a47d6af40d77d8eabc557f'
    with zipfile.ZipFile(apk) as z:
        assert not any(n.lower().endswith(('.jpg','.jpeg')) for n in z.namelist())
        assert all('lib/arm64-v8a/'+n in z.namelist() for n in
            ('libonnxruntime.so','libonnxruntime4j_jni.so','libmlkit_google_ocr_pipeline.so','libopencv_java4.so'))
        assert all(hashlib.sha256(z.read('assets/models/'+n)).hexdigest()==h for n,h in models.items())
        chars=json.loads(z.read('assets/models/english-characters.json'));assert len(chars)==438
    numeric=lambda r:[{k:i.get(k,True) for k in ('unit','count','quantityKnown','printedTotal','baseTotal','amountBased','includedDiscount')} for i in r['items']]
    changes=[]; allrows=[]
    for suffix,count in [('63',63),('11',11)]:
        rows=read('raw/'+a.current+'-app'+suffix+'.json');old={r['file']:r for r in read('raw/'+a.baseline+'-app'+suffix+'.json')}
        assert len(rows)==count and set(old)=={r['file'] for r in rows}
        for r in rows:
            b=old[r['file']];assert 'error' not in r
            assert r['method'].startswith('Paddle 한국어 OCR'),r['file']
            assert not any('품목명 보완 읽기를 완료하지 못했습니다' in w for w in r.get('warnings',[])),r['file']
            assert r['apkSHA256']==apk_hash,r['file']
            assert r['originalRows']==b['originalRows'],r['file']
            changed={}
            if r.get('total')!=b.get('total'):changed['total']=[b.get('total'),r.get('total')]
            if numeric(r)!=numeric(b):changed['fields']=[numeric(b),numeric(r)]
            names=lambda row:[i['name'] for i in row['items']]
            if names(r)!=names(b):changed['names']=[names(b),names(r)]
            if changed:changes.append(dict(cohort=suffix,file=r['file'],changes=changed))
        allrows+=rows
    ms=[r['milliseconds'] for r in allrows[:63]]
    nested_errors=sum(diagnostic_errors(r.get(k,[])) for r in allrows for k in ('nameDiagnostics','numericDiagnostics'))
    assert nested_errors==0, f'OCR diagnostic errors: {nested_errors}'
    result=dict(version=a.version,processed=len(allrows),errors=0,originals_verified=63,
        original11_assets_identical=True,
        diagnostic_errors=nested_errors,
        original_ocr_rows_unchanged=74,prior_apk_preserved=True,arm64_libraries_verified=True,models_verified=models,
        bytes=apk.stat().st_size,sha256=digest(apk),changes=changes,
        scan63_milliseconds=dict(total=sum(ms),median=statistics.median(ms),min=min(ms),max=max(ms)))
    (ROOT/(a.current+'-verification.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps({k:v for k,v in result.items() if k!='changes'},ensure_ascii=False,indent=2))
