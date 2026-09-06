"""Test removing the intermediate resize: source page -> model-height quad once."""
import json,math,time
import cv2,numpy as np,onnxruntime as ort
from PIL import Image,ImageOps
from region_data import ROOT,RAW,load,cases
from menu_images import patch
from menu_ctc_spans import infer,aligned

def decoded(engine,chars,crop):
    values,_=infer(engine,crop);tokens=aligned(values,chars)
    scores=[float(values[t['first'],chars.index(t['text'])]) for t in tokens]
    return dict(text=''.join(t['text'] for t in tokens),score=sum(scores)/len(scores) if scores else 0)

def direct(page,points,mode):
    q=np.floor(np.float32(points)+.5)
    w=max(np.linalg.norm(q[1]-q[0]),np.linalg.norm(q[2]-q[3]))
    h=max(np.linalg.norm(q[3]-q[0]),np.linalg.norm(q[2]-q[1]))
    width=max(1,math.ceil(w*48/h));dst=np.float32([[0,0],[width,0],[width,48],[0,48]])
    return cv2.warpPerspective(page,cv2.getPerspectiveTransform(q,dst),(width,48),flags=mode,borderValue=(255,255,255))

if __name__=='__main__':
    opt=ort.SessionOptions();opt.intra_op_num_threads=2;opt.log_severity_level=3
    engine=ort.InferenceSession(str(ROOT/'app/src/main/assets/models/korean.onnx'),opt,providers=['CPUExecutionProvider'])
    chars=load(ROOT/'app/src/main/assets/models/characters.json');pages={};rows=[]
    for case in cases():
        file=case['file']
        if file not in pages:pages[file]=cv2.cvtColor(np.array(ImageOps.exif_transpose(Image.open(case['source'])).convert('RGB')),cv2.COLOR_RGB2BGR)
        page=pages[file];start=time.perf_counter();views={}
        views['base']=decoded(engine,chars,patch(page,case['quad']))
        for name,mode in [('linear',cv2.INTER_LINEAR),('cubic',cv2.INTER_CUBIC),('lanczos',cv2.INTER_LANCZOS4)]:
            views[name]=decoded(engine,chars,direct(page,case['quad'],mode))
        rows.append(dict(case,views=views,seconds=time.perf_counter()-start))
    (RAW/'region-warp.json').write_text(json.dumps(dict(method='Page quad warped directly to 48px model height; no intermediate crop resize',rows=rows),ensure_ascii=False,indent=2,default=lambda v:v.item()),encoding='utf-8')
    print(len(rows),'regions')
