"""Compare actual Android ML Kit output with manually transcribed sample labels."""
import collections
import json
import pathlib
import re
import sys

sys.stdout.reconfigure(encoding='utf-8')

root = pathlib.Path(__file__).resolve().parents[1]
expected = json.loads((root / 'evidence/expected.json').read_text(encoding='utf-8'))
actual = json.loads((root / 'evidence/raw/batch.json').read_text(encoding='utf-8-sig'))
by_file = {r['file']: r for r in actual}
assert len(by_file) == len(expected), '11장 OCR 실행 완료 후 다시 평가하세요.'
assert all('error' not in r for r in actual), 'OCR 실행 오류를 먼저 확인하세요.'
normalize = lambda s: re.sub(r'[^가-힣a-z0-9]', '', s.lower())
lines = ['# 제공 영수증 실험 결과', '',
    'Android 에뮬레이터에서 ML Kit 한국어 16.0.1 실행. 수기 기준값과 비교.',
    '환경: Android 11/API 30, x86_64, WHPX, 2코어, RAM 2GB. 원본 EXIF 방향 반영.',
    '이 11장은 개발에 사용한 표본이며 새로운 영수증 정확도나 실제 휴대폰 속도를 의미하지 않습니다.',
    '금액 0 옵션 제외. 엄격 일치는 품목명(공백·기호 제외), 단가, 수량 모두 일치한 항목입니다.', '',
    '| 번호 | 기대/추출 행 | 엄격 일치 | 금액·수량 일치 | 기대/추출 합계 |',
    '|---|---:|---:|---:|---:|']
for e in expected:
    a = by_file[e['file']]
    items = [i for i in a.get('items', []) if i['unit'] != 0]
    gold = collections.Counter((normalize(n), u, q) for n, u, q in e['items'])
    got = collections.Counter((normalize(i['name']), i['unit'], i['count']) for i in items)
    gold_money = collections.Counter((u, q) for _, u, q in e['items'])
    got_money = collections.Counter((i['unit'], i['count']) for i in items)
    strict = sum((gold & got).values())
    money = sum((gold_money & got_money).values())
    total = sum(i['unit'] * i['count'] for i in items)
    lines.append(f"| {e['file']} | {len(e['items'])}/{len(items)} | {strict} | {money} | {e['total']:,}/{total:,} |")
lines += ['', '품목 없는 04·11의 추출 합계 0은 정상입니다. 결제 합계와 품목 합계는 구분합니다.',
    '상세 OCR 원문과 행 연결 결과: 로컬 evidence/raw/batch.json (개인정보 포함, Git 제외).']
for e in expected:
    a = by_file[e['file']]
    lines += ['', '## ' + e['file'] + ' — ' + e['source'], '',
        '| 실제 추출 이름 | 단가 | 수량 | 확인 사항 |', '|---|---:|---:|---|']
    for i in a.get('items', []):
        name = i['name'].replace('|', '/').replace('\t', ' ')
        lines.append(f"| {name} | {i['unit']:,} | {i['count']} | {i['warning']} |")
    lines += ['', ' / '.join(a.get('warnings', [])) or '계산 합계 경고 없음. 품목명 정확성을 보장하지 않습니다.']
(root / 'evidence/RESULTS.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
print('\n'.join(lines))
