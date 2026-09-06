"""Build local image inputs, optionally cropped from automatic OCR geometry."""
import base64
import io
import json
from pathlib import Path
import re
import statistics
from PIL import Image, ImageOps
from menu_glm_download import ROOT

def inputs(prompt,crop,limit):
    detected=json.loads((ROOT/'evidence/raw/menu-pc-probe.json').read_text(encoding='utf-8'))['rows']
    scans=Path('C:/Users/surromind/Downloads/Document scans')
    files=[ROOT/'app/src/androidTest/assets/receipts/03.jpg']+[scans/name for name in [
        'Scan_20260520_191858.jpg','Scan_20260715_130133.jpg',
        'Scan_20260723_124307.jpg','Scan_20260730_125509.jpg']]
    for file in files[:limit]:
        image=ImageOps.exif_transpose(Image.open(file)).convert('RGB'); encoded=io.BytesIO()
        box=None
        if crop:
            quads=[r['quad'] for r in detected if r['file']==file.name and len(re.findall('[가-힣]',r['old']))>=2]
            heights=[max(p[1] for p in q)-min(p[1] for p in q) for q in quads]
            margin=statistics.median(heights)*1.5
            box=[0,max(0,int(min(p[1] for q in quads for p in q)-margin)),image.width,
                 min(image.height,int(max(p[1] for q in quads for p in q)+margin))]
            image=image.crop(box)
        image.save(encoded,format='PNG')
        payload={'messages':[{'role':'user','content':[
            {'type':'image_url','image_url':{'url':'data:image/png;base64,'+base64.b64encode(encoded.getvalue()).decode()}},
            {'type':'text','text':prompt}]}], 'temperature':0,'max_tokens':4096,'seed':0}
        yield file,image.size,box,payload
