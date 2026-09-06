"""Execution coverage is separate from independently transcribed sample accuracy."""
import json
import re
from pathlib import Path
from evaluate_engines import matches
from evaluate_large_ocr import lines

ROOT=Path(__file__).resolve().parents[1]/'evidence'
def read(name):
    return json.loads((ROOT/name).read_text(encoding='utf8'))

if __name__=='__main__':
    manifest=read('document-scans-manifest.json')
    gold=read('document-scans-sample.json')
    ml=read('raw/document-scans-ml.json')
    surya=read('raw/document-scans-surya.json')
    paddle=read('raw/document-scans-paddle.json')
    expected={x['file'] for x in manifest}
    assert len(ml)==len(surya)==len(paddle)==len(expected)==63
    assert {x['file'] for x in ml}=={x['file'] for x in surya}==expected
    assert {x['file'] for x in paddle}==expected
    rows=[]; details=[]; scores=[0,0,0,0]; count=0; totals=0; paddle_totals=0; completed_names=0
    korean=[0,0,0,0]
    kr=lambda s:re.sub('[^가-힣]','',s)
    for g in gold:
        a=next(x for x in ml if x['file']==g['file'])
        b=next(x for x in surya if x['file']==g['file'])
        p=next(x for x in paddle if x['file']==g['file'])
        texts=[(a.get('originalRows') or a.get('rows','')).splitlines(),
               a.get('rows','').splitlines(),lines(b['text']),[l['text'] for l in p['lines']]]
        found=[matches(g['names'],t) for t in texts]
        k=[sum(matches([kr(n) for n in g['names']],[kr(s) for s in t])) for t in texts]
        korean=[x+y for x,y in zip(korean,k)]
        for i,name in enumerate(g['names']):
            details.append(f"| {g['file']} | {name} | "+' | '.join('O' if f[i] else 'X' for f in found)+' |')
        s=[sum(f) for f in found];scores=[x+y for x,y in zip(scores,s)]
        if not b['truncated']: completed_names+=s[2]
        count+=len(g['names']);totals+=a.get('total')==g['total']
        paddle_totals+=p.get('total')==g['total']
        rows.append(f"| {g['file']} | {len(g['names'])} | "+' | '.join(map(str,s))+' |')
    report=['# Document scans 실측','',
        f'전체 63장 실행. Android 예외 {sum("error" in x for x in ml)}장, Surya 출력 한도 도달 {sum(x["truncated"] for x in surya)}장.',
        '파일명 정렬 후 0,6,…,60,62번을 선정하여 원본을 직접 읽은 12장만 품목명 정답을 작성했습니다.',
        '양수 품목명 전체의 줄 내 일치(공백·기호 제외), 동일 줄 재사용 금지. 할인·0원 옵션 제외.',
        'ML 원본/앱 처리 후 텍스트를 구분합니다. Surya는 HTML 행/문단을 정리합니다. 품목·단가·수량 연결 정확도가 아닙니다.',
        f'표본 품목명: ML 원본 {scores[0]}/{count}, ML 앱 처리 {scores[1]}/{count}, Surya {scores[2]}/{count}, Paddle {scores[3]}/{count}.',
        f'보조 진단: 이름의 한글 부분만 전체 일치하는 수(같은 순서) {korean}/{count}. 영문·숫자 오류를 무시한 값이며 문자 정확도가 아닙니다.',
        f'Surya는 출력 한도 도달 여부와 별개로 이름 포함을 평가했습니다. 한도 도달 파일을 0 처리하면 {completed_names}/{count}입니다.',
        f'표본 총액 일치: ML 앱 {totals}/12장, Paddle {paddle_totals}/12장. Surya 총액은 별도 추출하지 않았습니다.','',
        '| 파일 | 품목명 수 | ML 원본 | ML 앱 처리 | Surya | Paddle |','|---|---:|---:|---:|---:|---:|']+rows+[
        '', '| 파일 | 기준 품목명 | ML 원본 | ML 앱 처리 | Surya | Paddle |','|---|---|---|---|---|---|']+details
    (ROOT/'DOCUMENT_SCANS_RESULTS.md').write_text('\n'.join(report)+'\n',encoding='utf8')
    print(scores,count,totals)
