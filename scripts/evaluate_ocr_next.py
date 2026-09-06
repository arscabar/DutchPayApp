"""Fixed names plus independently transcribed quantities and printed amounts."""
import json
from pathlib import Path
from evaluate_engines import matches, norm

ROOT=Path(__file__).resolve().parents[1]/'evidence'
def read(name):
    return json.loads((ROOT/name).read_text(encoding='utf8'))

if __name__=='__main__':
    gold=read('document-scans-amounts.json')['receipts']
    old=read('document-scans-sample.json')
    assert len(gold)==len(old)==12 and sum(len(g['items']) for g in gold)==34
    assert all([i['name'] for i in a['items']]==b['names'] for a,b in zip(gold,old))
    assert all(sum(i['amount'] for i in g['items'])+sum(d['amount'] for d in g['discounts']
               if d['include_in_total'])==g['total'] for g in gold)
    table=[]; details=[]
    configs=[('v5 / 1600','document-scans-paddle.json'),
             ('v5 / 2400','document-scans-paddle-2400.json'),
             ('v6 small / 1600','document-scans-paddle-v6.json'),
             ('v6 + 파서 수정','document-scans-paddle-v6-reparsed.json')]
    for label,file in configs:
        data=read('raw/'+file);by={d['file']:d for d in data}
        assert len(data)==len(by)==63
        assert set(by)=={d['file'] for d in read('document-scans-manifest.json')}
        if 'reparsed' in file:
            assert all(by[d['file']]['lines']==d['lines'] for d in read('raw/document-scans-paddle-v6.json'))
        score=totals=triples=sums=0;has_amount=any('printedTotal' in i for d in data for i in d['items'])
        for g in gold:
            d=by[g['file']];names=[i['name'] for i in g['items']]
            n=sum(matches(names,[l['text'] for l in d['lines']]));score+=n
            totals+=d.get('total')==g['total'];available=list(d['items']);t=0
            sums+=sum(i['unit']*i['count'] for i in available)==g['total']
            if 'reparsed' in file and sum(i['unit']*i['count'] for i in available)!=g['total']:
                assert d['warnings'], 'Incorrect calculated total must require review'
            for e in g['items']:
                j=next((j for j,i in enumerate(available) if norm(i['name'])==norm(e['name'])
                    and i['count']==e['quantity'] and i.get('printedTotal')==e['amount']),None)
                if j is not None:t+=1;available.pop(j)
            triples+=t
            details.append(f"| {label} | {g['file']} | {n}/{len(names)} | {t if has_amount else '미기록'} |")
        table.append(f"| {label} | {score}/34 | {totals}/12 | {str(triples)+'/34' if has_amount else '미기록'} | {sums}/12 |")
    report=['# OCR 개선 후보 실측','',
        '같은 스캔 63장 실행. 고정 표본 12장, 양수 품목 34개. 원본·정답은 모델 입력에 사용하지 않았습니다.',
        'OCR 이름은 공백·기호를 제외한 전체 이름 포함. 연결 평가는 파서 품목명·수량·인쇄 행 금액이 모두 정확히 일치해야 합니다.',
        '예전 출력에는 인쇄 행 금액 필드가 없어 연결 점수를 소급 계산하지 않습니다. 할인 정확도는 이 34개 지표에 포함하지 않습니다.',
        'Android 에뮬레이터 결과입니다. v6는 공식 검출 임계값도 변경되므로 검출 모델+설정 조합 비교입니다.','',
        '| 검출기 / 긴 변 제한 | OCR 품목명 | 결제 총액 | 품목명·수량·인쇄금액 연결 | 추출 항목 계산합계 |','|---|---:|---:|---:|---:|']+table+[
        '', '| 조건 | 파일 | OCR 품목명 | 연결 일치 수 |','|---|---|---:|---:|']+details
    (ROOT/'OCR_NEXT_RESULTS.md').write_text('\n'.join(report)+'\n',encoding='utf8')
    print('\n'.join(table))
