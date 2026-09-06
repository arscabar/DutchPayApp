"""Fixed name-coverage comparison of local document OCR candidates."""
import html
import json
import re
import statistics
from pathlib import Path
from evaluate_engines import matches

ROOT = Path(__file__).resolve().parents[1]/'evidence'
def read(name):
    return json.loads((ROOT/name).read_text(encoding='utf-8-sig'))

def lines(text):
    if '<div' in text:
        text = re.sub(r'\s+', ' ', text)
    text = re.sub(r'<\|LOC_\d+\|>', '', text)
    text = re.sub(r'</(?:tr|p|div|li|h[1-6])>|<br\s*/?>', '\n', text)
    return html.unescape(re.sub(r'<[^>]+>', ' ', text)).splitlines()

if __name__ == '__main__':
    assert sum(matches(['밥','국'],lines('<tr><td>밥</td></tr><tr><td>국</td></tr>')))==2
    gold = read('expected.json')
    rows,details = [],[]
    for label,file in [('PaddleOCR-VL OCR','vl.json'),
                       ('PaddleOCR-VL Spotting','vl-spotting.json'),
                       ('Surya 2','surya.json')]:
        data = read('raw/'+file)
        assert len(data)==11 and {a['file'] for a in data}=={g['file'] for g in gold}
        count=0
        failures=[]
        for g in gold:
            a=next(x for x in data if x['file']==g['file'])
            names=[n for n,u,q in g['items'] if u>0]
            found=matches(names,[] if a['truncated'] else lines(a['text']))
            count+=sum(found)
            if a['truncated']: failures.append(a['file'])
            details.append(f"| {label} | {g['file']} | {sum(found)}/{len(names)} |")
        rows.append(f'| {label} | {count}/27 | {statistics.median(a["seconds"] for a in data):.2f}초 | {", ".join(failures) or "없음"} |')
    report=['# 문서 OCR 모델 비교','',
        '동일 원본 11장. 전체 품목명이 한 출력 줄에 포함되는지 평가(공백·기호 제외, 줄 중복 사용 금지).',
        'HTML 표 행/문단 경계와 LOC 위치 토큰만 정리했습니다. 출력 한도 도달 파일은 0점입니다.',
        '양수 품목 27개 대상이며, 가격·수량 연결·허위 품목·전체 문자 정확도는 평가하지 않았습니다.',
        '기존 Android ML Kit는 21/27, Paddle mobile 1600은 22/27입니다.',
        '아래 모델은 PC RTX 3060 실행입니다. Android 속도와 직접 비교할 수 없습니다.','',
        '| 모델 | 품목명 일치 | 중앙 시간 | 출력 한도 도달 |',
        '|---|---:|---:|---|']+rows+['',
        '| 모델 | 파일 | 품목명 일치 |','|---|---|---:|']+details
    (ROOT/'LARGE_OCR_RESULTS.md').write_text('\n'.join(report)+'\n',encoding='utf8')
    print('\n'.join(rows))
