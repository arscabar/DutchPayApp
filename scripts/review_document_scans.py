"""Local-only review: escape OCR output; never execute generated HTML."""
import html
import json
import base64
from io import BytesIO
from pathlib import Path
from PIL import Image, ImageOps
from evaluate_large_ocr import lines

import os
ROOT=Path(__file__).resolve().parents[1]/'evidence'
_DEFAULT=Path(os.environ.get('SCAN_DIR',r'C:\Users\박경민\Downloads\Document scans'))
SOURCE=_DEFAULT if _DEFAULT.exists() else Path('C:/Users/surromind/Downloads/Document scans')
def read(name):
    return json.loads((ROOT/name).read_text(encoding='utf8'))

if __name__=='__main__':
    ml={x['file']:x for x in read('raw/document-scans-ml.json')}
    surya={x['file']:x for x in read('raw/document-scans-surya.json')}
    paddle={x['file']:x for x in read('raw/document-scans-paddle.json')}
    cards=[]
    for x in read('document-scans-manifest.json'):
        name=x['file'];a=ml.get(name,{});b=surya.get(name,{})
        with Image.open(SOURCE/name) as source:
            preview=ImageOps.exif_transpose(source).convert('RGB')
            preview.thumbnail((700,2000));buffer=BytesIO()
            preview.save(buffer,format='JPEG',quality=80)
        image='data:image/jpeg;base64,'+base64.b64encode(buffer.getvalue()).decode('ascii')
        items='\n'.join(f"{i['name']} | 단가 {i['unit']} | 수량 {i['count']}" for i in a.get('items',[]))
        left=a.get('rows','미완료')+'\n\n[앱 추출 품목]\n'+(items or '없음')
        right='\n'.join(s for s in lines(b.get('text','미완료')) if s.strip())
        other='\n'.join(l['text'] for l in paddle.get(name,{}).get('lines',[]))
        if b.get('truncated'):right='[출력 한도 도달]\n'+right
        cards.append(f'<details><summary>{html.escape(name)}</summary><div class="grid">'
            f'<section><h3>원본</h3><a href="{(SOURCE/name).as_uri()}">원본 열기</a>'
            f'<img loading="lazy" src="{image}" alt="스캔 영수증 미리보기"></section>'
            f'<section><h3>Android ML Kit + 기존 처리</h3><pre>{html.escape(left)}</pre></section>'
            f'<section><h3>Surya OCR</h3><pre>{html.escape(right)}</pre>'
            f'<h3>Paddle OCR</h3><pre>{html.escape(other)}</pre></section></div></details>')
    page='''<!doctype html><html lang="ko"><meta charset="utf-8"><title>스캔 OCR 대조</title>
<style>body{font:16px sans-serif;margin:24px;background:#f3f5f7;color:#18202b}details{background:white;padding:16px;margin:12px 0;border-radius:8px}summary{cursor:pointer;font-weight:bold}.grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}img{width:100%}pre{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.6}section{max-height:850px;overflow:auto}@media(max-width:850px){.grid{grid-template-columns:1fr}}</style>
<h1>스캔 영수증 63장 OCR 대조</h1><p>파일명을 눌러 원본과 출력을 비교하세요. 품목 인식 성공을 보증하는 자료는 아닙니다. 원본 경로는 이 PC에서만 열립니다.</p>'''
    (ROOT/'raw/document-scans-review.html').write_text(page+'\n'.join(cards)+'</html>',encoding='utf8')
