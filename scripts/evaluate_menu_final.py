"""Generate final field comparison only from complete 63/11 receipt app exports."""
import argparse
import hashlib
import json
from pathlib import Path
from evaluate_names import read
from evaluate_menu_compare import cohort, financial_changes
from evaluate_menu_report import render

def run(evidence, baseline='names-app', current='menus-app', output='MENU', title='v0.5 → v0.6 메뉴 필드 비교'):
    inputs, snapshots = {}, {}
    for n in (63,11):
        for version in (baseline,current):
            p=evidence/'raw'/f'{version}{n}.json'
            raw=read(p); by={r['file']:r for r in raw}
            assert len(raw)==len(by)==n, f'Incomplete or duplicate input: {p}'
            assert all('error' not in r for r in raw), f'Error record: {p}'
            inputs[version,n]=(p,raw)
            snapshots[str(p)]=hashlib.sha256(p.read_bytes()).hexdigest()
        assert {r['file'] for r in inputs[baseline,n][1]}=={r['file'] for r in inputs[current,n][1]}
    app_hashes={r.get('apkSHA256') for n in (63,11) for r in inputs[current,n][1]}
    assert len(app_hashes)==1 and all(isinstance(h,str) and len(h)==64 for h in app_hashes), 'Missing or mixed current APK hashes'
    result={'apk_sha256':next(iter(app_hashes)),'input_sha256':snapshots,'gold_sha256':{},'cohorts':{},'financial_changes':{},'notes':[
        '각 표본군은 중복될 수 있다. 특히 원본05는 고정63의 CU 사진과 같으므로 합산 정확도를 만들지 않는다.',
        '이름 채점은 기존 norm(NFC 후 공백·기호 제외, 대소문자 유지)과 같다. 한자 등 제외 문자가 있는 이름은 별도 원문 대조한다.',
        '행 연결은 숫자 없이 이름 편집거리로 순서를 보존한다. 행 수가 달라진 연결과 반복 이름은 수동 검토한다.',
        '추가6장은 파일명 SHA-256으로 고르고 OCR 출력을 보지 않고 전사했지만 같은63장 개발 집합의 사후 회귀 자료다.',
        '추가6장의 음수 구성품 제거4행과 할인은 양수 필드 표에 포함하지 않으며 별도 원본 대조한다.',
        '기존11장 별도 gold의06은 인쇄 단가가 불명확해 단가를 미평가하며 수량/행금액만 평가한다.',
        '합계 일치는 품목/숫자 필드 정확도를 대체하지 않는다. 수동 후보 목록도 자동 정답으로 세지 않는다.'
        ,'quantityKnown=false인 수량은 내부 표식0 대신 미인식(null)으로 채점한다. 관측 단가는 유지하되 baseTotal은 인쇄 행금액일 수 있다.'
        ,'이름 필드 일치는 norm 기준이다. CER는 NFC·공백 정리 후 기호·한자·행바꿈을 포함한 원문 문자열의 편집거리로 계산하므로 이름 필드 정확도와 다르다.'
    ]}
    specs=[('고정 양수34',63,'document-scans-amounts.json','items','positive'),
           ('고정 무료3',63,'document-scans-amounts.json','zero_rows','zero'),
           ('원본03 옵션19',11,'STRUCTURE_GOLD.json','items','nonnegative'),
           ('기존11 중03 제외',11,'MENU_LEGACY_GOLD.json','items','positive'),
           ('추가07/29·회전',63,'MENU_EXTRA_GOLD.json','items','positive'),
           ('추가6장 양수',63,'MENU_ADDITIONAL_GOLD.json','items','positive')]
    for label,n,gold,section,kind in specs:
        result['gold_sha256'][gold]=hashlib.sha256((evidence/gold).read_bytes()).hexdigest()
        result['cohorts'][label]=cohort(inputs[baseline,n][0],inputs[current,n][0],evidence/gold,section,kind)
    for n in (63,11):
        result['financial_changes'][str(n)]=financial_changes(inputs[baseline,n][1],inputs[current,n][1])
    review_path=evidence/f'{output}_NUMBER_REVIEW.json'
    if review_path.exists():
        review=read(review_path)
        if review.get('status')=='complete' and review.get('input_sha256')==snapshots:
            assert review['apkSHA256']==result['apk_sha256'], 'Review APK differs'
            result['number_review']=review
    for p,digest in snapshots.items():
        assert hashlib.sha256(Path(p).read_bytes()).hexdigest()==digest, f'Input changed while reading: {p}'
    for gold,digest in result['gold_sha256'].items():
        assert hashlib.sha256((evidence/gold).read_bytes()).hexdigest()==digest, f'Gold changed: {gold}'
    result['title']=title;result['output_prefix']=output
    (evidence/f'{output}_FINAL_FIELDS.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
    (evidence/f'{output}_FIELD_COMPARISON.md').write_text(render(result),encoding='utf8')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--evidence',type=Path,default=Path(__file__).resolve().parents[1]/'evidence')
    p.add_argument('--baseline',default='names-app');p.add_argument('--current',default='menus-app')
    p.add_argument('--output',default='MENU');p.add_argument('--title',default='v0.5 → v0.6 메뉴 필드 비교')
    a=p.parse_args();run(a.evidence,a.baseline,a.current,a.output,a.title)
