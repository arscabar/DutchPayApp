"""Compare fixed hand-labelled references against both real Android runs."""
import collections as c
import json
import pathlib
import re
import statistics
import sys

sys.stdout.reconfigure(encoding='utf-8')
root = pathlib.Path(__file__).resolve().parents[1]
read = lambda p: json.loads((root / p).read_text(encoding='utf-8-sig'))
gold = read('evidence/expected.json')
old = {x['file']: x for x in read('evidence/raw/v1.json')}
new = {x['file']: x for x in read('evidence/raw/batch.json')}
assert set(old) == set(new) == {x['file'] for x in gold}
assert all('error' not in x for x in new.values())
norm = lambda s: re.sub(r'[^가-힣a-z0-9]', '', s.lower())

def measure(e, a):
    expected = c.Counter((norm(n), u, q) for n, u, q in e['items'])
    got = c.Counter((norm(i['name']), i['unit'], i['count'])
                    for i in a['items'] if i['unit'] != 0)
    money = c.Counter((u, q) for _, u, q in expected.elements())
    observed = c.Counter((u, q) for _, u, q in got.elements())
    return sum((expected & got).values()), expected == got, money == observed

rows = []; totals = [0, 0]; complete = [0, 0]; financial = [0, 0]
for e in gold:
    a, b = measure(e, old[e['file']]), measure(e, new[e['file']])
    if e['items']:
        for i, m in enumerate([a, b]):
            totals[i] += m[0]; complete[i] += m[1]; financial[i] += m[2]
    rows.append(f"| {e['file']} | {len(e['items'])} | {a[0]} → {b[0]} | {new[e['file']].get('method', '')} |")
text = ['# OCR 개선 전후 비교', '',
    f'품목명·단가·수량 일치 항목: **{totals[0]}/30 → {totals[1]}/30**.',
    f'모든 품목이 일치한 영수증: **{complete[0]}/9 → {complete[1]}/9**.',
    f'단가·수량 조합 전체 일치: **{financial[0]}/9 → {financial[1]}/9**.', '',
    '단가·수량 조합 지표는 순서를 무시하므로 이름과 가격의 연결 정확도를 단독으로 보장하지 않습니다.',
    '품목명은 공백·기호만 제외합니다. 할인 명칭 차이도 엄격 일치에서는 오답으로 계산합니다.',
    '같은 11장으로 개발·검증한 결과이며 새로운 영수증에 대한 일반 정확도가 아닙니다.',
    '품목 없는 전표 2장은 품목 평가에서 제외합니다. 정답 파일은 변경하지 않았습니다.', '',
    '| 번호 | 기준 항목 수 | 정확한 항목: 이전 → 개선 | 처리 방식 |',
    '|---|---:|---:|---|', *rows, '',
    f"에뮬레이터 처리 중앙값: {statistics.median(x['milliseconds'] for x in old.values())/1000:.1f}초 → {statistics.median(x['milliseconds'] for x in new.values())/1000:.1f}초.",
    '실제 휴대폰 속도는 미측정. 품목 재인식으로 처리 시간이 늘어납니다.']
(root / 'evidence/COMPARISON.md').write_text('\n\n'.join(text[:1])+'\n'+'\n'.join(text[1:])+'\n', encoding='utf-8')
print('\n'.join(text))
