"""Compare fixed receipt labels with actual Android full detector pipeline output."""
import json
import re
import statistics
from collections import Counter
from pathlib import Path
from evaluate_engines import matches

ROOT = Path(__file__).resolve().parents[1]
def read(name):
    return json.loads((ROOT/'evidence'/name).read_text(encoding='utf-8-sig'))
def norm(s):
    return re.sub(r'[^가-힣a-z0-9]', '', s.lower())
def score(gold, actual):
    g = Counter((norm(n), u, q) for n,u,q in gold['items'])
    a = Counter((norm(i['name']), i['unit'], i['count']) for i in actual['items'] if i['unit'])
    return sum((g&a).values()), sum((a-g).values()), sum(a.values())

if __name__ == '__main__':
    assert score({'items':[['밥',9000,1]]}, {'items':[{'name':'밥','unit':9000,'count':2}]}) == (0,1,1)
    gold = read('expected.json')
    baseline_lines = read('raw/engines.json')
    variants = [('ML Kit 0.2',read('raw/batch.json')),
                ('Paddle 960',read('raw/full-960.json')),('Paddle 1600',read('raw/full-1600.json'))]
    report = ['# Paddle 검출 + 한국어 인식 Android 실측','',
        '동일한 11장, 기존 정답 30항목. 공백·기호·영문 대소문자만 무시한 품목명·단가·수량 엄격 일치.',
        '오추출은 정답 삼중항과 일치하지 않는 출력 수입니다. 이름 오독도 오추출로 계산합니다.',
        '가격 0 옵션은 제외. 실기기 속도·새 영수증 일반화는 미검증.', '',
        '| 방식 | 품목명 /27 | 정확한 항목 /30 | 오추출 | 추출 수 | 전체 품목 일치 /9 | 중앙 시간 |',
        '|---|---:|---:|---:|---:|---:|---:|']
    details=[]
    for label,data in variants:
        assert {x['file'] for x in data} == {g['file'] for g in gold}
        assert all('error' not in x for x in data)
        tp=fp=count=complete=names=0
        details+=['',f'## {label}','','| 파일 | 정확 / 기준 | 오추출 | 품목 합계 / 결제 기준 |','|---|---:|---:|---:|']
        for g in gold:
            a=next(x for x in data if x['file']==g['file'])
            texts = [l['text'] for l in a['lines']] if 'lines' in a else [
                l['ml'] for l in next(x for x in baseline_lines if x['file']==g['file'])['lines']]
            names+=sum(matches([n for n,u,q in g['items'] if u>0],texts))
            t,f,n=score(g,a);tp+=t;fp+=f;count+=n
            complete+=bool(g['items']) and t==len(g['items']) and f==0
            amount=sum(i['unit']*i['count'] for i in a['items'])
            details.append(f"| {g['file']} | {t}/{len(g['items'])} | {f} | {amount}/{g['total']} |")
        median=statistics.median(x['milliseconds'] for x in data)/1000
        report.append(f'| {label} | {names} | {tp} | {fp} | {count} | {complete} | {median:.2f}초 |')
    report+=['','품목 없는 카드전표의 품목 합계 0은 정상이며 결제금액과 구분해야 합니다.']+details
    (ROOT/'evidence/FULL_PADDLE_RESULTS.md').write_text('\n'.join(report)+'\n',encoding='utf8')
    print('\n'.join(report[:13]))
