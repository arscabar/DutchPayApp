"""Local English-only OCR of CTC-selected image spans, never textual autocorrect."""
import json
import time
from pathlib import Path
import cv2
import numpy as np
import onnxruntime as ort
import yaml
from PIL import Image,ImageOps
from menu_probe import ROOT,RAW,load
from menu_images import patch
from menu_decode import decode
from menu_ctc_spans import infer,spans

if __name__=='__main__':
    options=ort.SessionOptions();options.intra_op_num_threads=2;options.log_severity_level=3
    engine=lambda p:ort.InferenceSession(str(p),options,providers=['CPUExecutionProvider'])
    korean=engine(ROOT/'app/src/main/assets/models/korean.onnx')
    english=engine(ROOT/'.tools/english-model/inference.onnx')
    kc=load(ROOT/'app/src/main/assets/models/characters.json')
    cfg=yaml.safe_load((ROOT/'.tools/english-model/inference.yml').read_text(encoding='utf8'))
    ec=['']+cfg['PostProcess']['character_dict']+[' ']
    result={'device':'PC CPU ORT, 2 threads','rows':[]}; pages={}
    for row in load(RAW/'menu-pc-probe.json')['rows']:
        file=row['file']
        if file not in pages:
            path=ROOT/'app/src/androidTest/assets/receipts/03.jpg' if file=='03.jpg' else Path('C:/Users/surromind/Downloads/Document scans')/file
            pages[file]=cv2.cvtColor(np.array(ImageOps.exif_transpose(Image.open(path)).convert('RGB')),cv2.COLOR_RGB2BGR)
        crop=patch(pages[file],row['quad']);h,w=crop.shape[:2]
        values,full_width=infer(korean,crop)
        for span in spans(values,kc,full_width,w):
            observed=dict(span,file=file,quad=row['quad'],variants={})
            for factor in [0,.1,.2]:
                left=max(0,span['x'][0]-round(h*factor));right=min(w,span['x'][1]+round(h*factor))
                image=crop[:,left:right];start=time.perf_counter()
                logits,_=infer(english,image);decoded=decode(logits,ec)
                observed['variants'][str(factor)]=dict(decoded,seconds=time.perf_counter()-start,x=[left,right])
            result['rows'].append(observed)
    (RAW/'menu-english-probe.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
    for row in result['rows']:print(row['file'],repr(row['old']),{k:(v['text'],round(v['score'],3)) for k,v in row['variants'].items()})
