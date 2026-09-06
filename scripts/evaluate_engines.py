"""Name coverage only: does a detected line contain the complete reference name?"""
import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KEYS = ['ml', 'mlFlat', 'paddleBox', 'paddleFlat', 'paddlePadded']

def norm(s):
    return re.sub(r'[^가-힣a-zA-Z0-9]', '', unicodedata.normalize('NFC', s))

def matches(names, lines):
    available = [norm(s) for s in lines]
    result = []
    for name in names:
        index = next((i for i, s in enumerate(available) if norm(name) in s), None)
        result.append(index is not None)
        if index is not None:
            available.pop(index)
    return result

if __name__ == '__main__':
    assert matches(['삶은계란 추가']*2, ['▶ 삶은계란 추가']) == [True, False]
    assert not matches(['김치찌개'], ['김치피개'])[0]
    gold = json.loads((ROOT/'evidence/expected.json').read_text(encoding='utf8'))
    data = json.loads((ROOT/'evidence/raw/engines.json').read_text(encoding='utf8'))
    padded = json.loads((ROOT/'evidence/raw/padded.json').read_text(encoding='utf8'))
    assert {x['file'] for x in data} == {x['file'] for x in gold}
    assert {x['file'] for x in padded} == {x['file'] for x in gold}
    total = [0]*len(KEYS)
    rows, details = [], []
    count = 0
    for expected in gold:
        actual = next(x for x in data if x['file'] == expected['file'])
        extra = next(x for x in padded if x['file'] == expected['file'])
        assert [s['ml'] for s in actual['lines']] == [s['ml'] for s in extra['lines']]
        names = [n for n, price, qty in expected['items'] if price > 0]
        count += len(names)
        found = [matches(names, [s[k] for s in (extra if k=='paddlePadded' else actual)['lines']]) for k in KEYS]
        sums = [sum(x) for x in found]
        total = [a+b for a,b in zip(total,sums)]
        rows.append('| '+expected['file']+' | '+str(len(names))+' | '+' | '.join(map(str,sums))+' |')
        for i,name in enumerate(names):
            details.append('| '+expected['file']+' '+name+' | '+' | '.join('O' if f[i] else 'X' for f in found)+' |')
    report = ['# Android OCR 엔진 비교 실측', '',
        '품목명 전체가 한 OCR 줄에 포함되는지 평가. 공백·기호만 제외하며 같은 줄은 중복 사용하지 않습니다.',
        '할인 3개를 제외한 양수 품목 27개 대상. 가격·수량 연결, 불필요한 항목 검출은 평가하지 않습니다.',
        '따라서 이전 22/30 품목·단가·수량 지표와 직접 비교하면 안 됩니다.', '',
        '| 영수증 | 대상 | ML Kit | ML 줄 보정 | Paddle | Paddle 줄 보정 | Paddle 여백 |',
        '|---|---:|---:|---:|---:|---:|---:|']+rows+[
        '| 합계 | '+str(count)+' | '+' | '.join(map(str,total))+' |', '',
        '| 기준 품목 | ML Kit | ML 줄 보정 | Paddle | Paddle 줄 보정 | Paddle 여백 |',
        '|---|---|---|---|---|---|']+details
    (ROOT/'evidence/ENGINE_RESULTS.md').write_text('\n'.join(report)+'\n',encoding='utf8')
    print(dict(zip(KEYS,total)), 'out of',count)
