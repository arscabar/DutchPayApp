"""Same name-coverage metric as previous engines; not item extraction accuracy."""
import json
import statistics
from pathlib import Path
from evaluate_engines import matches

ROOT = Path(__file__).resolve().parents[1]
def read(name):
    return json.loads((ROOT/'evidence'/name).read_text(encoding='utf-8-sig'))

if __name__ == '__main__':
    gold, actual = read('expected.json'), read('raw/vl.json')
    assert {g['file'] for g in gold} == {a['file'] for a in actual}
    failures = [a['file'] for a in actual if a['truncated']]
    baseline = read('raw/engines.json')
    paddle = read('raw/full-1600.json')
    totals = [0,0,0]
    rows = []
    for g in gold:
        names = [n for n,u,q in g['items'] if u>0]
        a = next(x for x in actual if x['file']==g['file'])
        b = next(x for x in baseline if x['file']==g['file'])
        p = next(x for x in paddle if x['file']==g['file'])
        texts = [[x['ml'] for x in b['lines']],
                 [x['text'] for x in p['lines']],
                 [] if a['truncated'] else a['text'].splitlines()]
        scores = [sum(matches(names,t)) for t in texts]
        totals = [x+y for x,y in zip(totals,scores)]
        rows.append('| '+g['file']+' | '+str(len(names))+' | '+
                    ' | '.join(map(str,scores))+' |')
    report = ['# 상위 OCR 모델 실측','',
        '같은 11장, 양수 품목명 27개. 공백·기호를 제외한 전체 이름이 한 출력 줄에 포함되어야 일치합니다.',
        '동일 줄은 한 번만 사용합니다. 가격·수량 연결과 허위 품목은 이 지표에 포함되지 않습니다.',
        'ML Kit/Paddle mobile은 이전 Android 에뮬레이터 결과, VL은 PC RTX 3060 결과입니다.','',
        '| 파일 | 품목명 수 | ML Kit | Paddle mobile 1600 | PaddleOCR-VL 1.6 |',
        '|---|---:|---:|---:|---:|']+rows+[
        '| 합계 | 27 | '+' | '.join(map(str,totals))+' |','',
        f"VL 처리시간 중앙값: {statistics.median(a['seconds'] for a in actual):.2f}초 (첫 실행 포함, 모델 로딩 제외).",
        f"출력 한도 도달 파일(품목명 점수 0 처리): {', '.join(failures) or '없음'}.",
        '이 결과는 전체 문자 정확도나 완성 앱 정확도가 아닙니다. 개발에 사용한 11장으로 일반화 성능은 미검증입니다.']
    (ROOT/'evidence/VL_RESULTS.md').write_text('\n'.join(report)+'\n',encoding='utf8')
    print(totals)
