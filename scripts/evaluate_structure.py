"""Compare frozen v0.3 outputs with structural parsing/OCR changes."""
from collections import Counter
from evaluate_steps import ROOT, read, amount
from evaluate_engines import norm

def key(i):return norm(i['name']),i['count'],i['printedTotal']

if __name__=='__main__':
    sample=read('document-scans-amounts.json')['receipts']
    legacy=read('expected.json')
    gold=read('STRUCTURE_GOLD.json')['receipts'][0]
    assert sum(i['unit']*i['count'] for i in gold['items'])==gold['grossTotal']
    assert sum(i['printedTotal'] for i in gold['items'])==gold['expectedTotal']
    lines=['# 영수증 구조 개선 검증','',
        '63장 중 고정 표본 12장·34개 품목을 평가하고, 처음 제공한 11장은 별도로 비교합니다.',
        '품목 연결은 이름(공백·기호 제외)·수량·인쇄 행 금액의 동시 일치입니다.',
        '개발에 사용한 표본이며, 계산합계 일치가 모든 품목과 수량의 정확성을 보장하지 않습니다.','',
        '| 단계 | 63장 표본 연결 | 표본 계산합계 | 기존11장 결제총액 | 품목 있는9장 계산합계 |',
        '|---|---:|---:|---:|---:|']
    detail=[]
    for label,prefix in [('이전 v0.3','steps'),('구조 개선','structure')]:
        if not (ROOT/'raw'/f'{prefix}-app63.json').exists():continue
        many=read(f'raw/{prefix}-app63.json');old=read(f'raw/{prefix}-app11.json')
        assert len(many)==63 and len(old)==11
        assert all('error' not in r for r in many+old)
        by={r['file']:r for r in many};known={r['file']:r for r in old}
        assert set(by)=={r['file'] for r in read('document-scans-manifest.json')}
        linked=balanced=0
        for g in sample:
            r=by[g['file']]
            linked+=sum((Counter(key(i) for i in r['items']) &
                Counter((norm(i['name']),i['quantity'],i['amount']) for i in g['items'])).values())
            balanced+=sum(amount(i) for i in r['items'])==g['total']
        totals=sum(known[g['file']].get('total')==g['total'] for g in legacy)
        calc=sum(sum(amount(i) for i in known[g['file']]['items'])==g['total']
            for g in legacy if g['items'])
        lines.append(f'| {label} | {linked}/34 | {balanced}/12 | {totals}/11 | {calc}/9 |')
        r=known['03.jpg']
        match=sum((Counter(key(i) for i in r['items']) & Counter(key(i) for i in gold['items'])).values())
        detail+=['',f'## {label} — 옵션 영수증',f'19개 인쇄 행 중 이름·수량·행 금액 일치: {match}/19.',
            f"계산합계 {sum(amount(i) for i in r['items']):,}원 / 결제총액 후보 {r.get('total')}원 / 정답 77,300원.",'',
            '| 추출 이름 | 단가/행 금액 | 수량 | 인쇄 행 금액 | 계산 반영 |','|---|---:|---:|---:|---:|']
        for i in r['items']:
            detail.append(f"| {i['name'].replace('|','/')} | {i['unit']} | {i['count']} | {i['printedTotal']} | {amount(i)} |")
        detail+=['','확인 경고: '+' / '.join(r['warnings'])]
    (ROOT/'STRUCTURE_RESULTS.md').write_text('\n'.join(lines+detail)+'\n',encoding='utf8')
    print('\n'.join(lines))
