"""Frozen sample; report linkage and extra rows separately from OCR coverage."""
import json
from pathlib import Path
from evaluate_engines import norm, matches

ROOT=Path(__file__).resolve().parents[1]/'evidence'
def read(path):return json.loads((ROOT/path).read_text(encoding='utf8'))
def amount(i):return i.get('baseTotal',i['unit']*i['count'])

if __name__=='__main__':
    gold=read('document-scans-amounts.json')['receipts']
    expected={d['file'] for d in read('document-scans-manifest.json')}
    report=['# 단계별 OCR 개선 검증','',
        '63장 실행, 고정 표본 12장·양수 품목 34개. 할인·0원 옵션은 연결 지표에서 제외합니다.',
        '연결은 품목명(공백·기호 제외)·수량·인쇄 행 금액이 모두 맞아야 합니다. 실제 계산합계는 이미 반영된 할인을 재차 빼지 않습니다.',
        '양수 추출 행 수는 과다 추출도 확인하기 위한 값으로, 숫자가 많을수록 좋은 지표가 아닙니다.','',
        '| 단계 | OCR 품목명 | 연결 일치 | 계산합계 일치 | 양수 추출 행 |',
        '|---|---:|---:|---:|---:|']
    details=[]
    for label,file in [('이전 파서','document-scans-paddle-v6-reparsed.json'),
                       ('좌표 연결','steps-layout.json'),('영역 재인식 실험','steps-retry.json'),
                       ('실제 앱 경로','steps-app63.json')]:
        if not (ROOT/'raw'/file).exists():continue
        raw=read('raw/'+file);data={d['file']:d for d in raw}
        assert len(raw)==len(data)==63 and set(data)==expected
        if file=='steps-layout.json':
            assert all(data[d['file']]['lines']==d['lines'] for d in read('raw/document-scans-paddle-v6.json'))
        names=linked=totals=positive=0
        for g in gold:
            d=data[g['file']];available=list(d['items']);found=0
            lines=[l['text'] for l in d['lines']] if 'lines' in d else d['rows'].splitlines()
            names+=sum(matches([e['name'] for e in g['items']],lines))
            for e in g['items']:
                j=next((j for j,i in enumerate(available) if norm(i['name'])==norm(e['name'])
                    and i['count']==e['quantity'] and i['printedTotal']==e['amount']),None)
                if j is not None:found+=1;available.pop(j)
            linked+=found;positive+=sum(amount(i)>0 for i in d['items'])
            total=sum(amount(i) for i in d['items']);totals+=total==g['total']
            details.append(f"| {label} | {g['file']} | {found}/{len(g['items'])} | {total} / {g['total']} |")
        report.append(f'| {label} | {names}/34 | {linked}/34 | {totals}/12 | {positive} |')
    report+=['','| 단계 | 파일 | 연결 | 계산합계 / 정답 |','|---|---|---:|---:|']+details
    (ROOT/'STEPS_RESULTS.md').write_text('\n'.join(report)+'\n',encoding='utf8')
    print('\n'.join(report[:12]))
