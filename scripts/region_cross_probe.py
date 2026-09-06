"""Use stored ML readings with actual PC CTC output. Android uses a separate capture."""
import json,time
import cv2,numpy as np,onnxruntime as ort
from PIL import Image,ImageOps
from region_data import ROOT,RAW,load,cases,key
from region_cross import choose
from menu_ctc_spans import infer,aligned
from menu_images import patch

options=ort.SessionOptions();options.intra_op_num_threads=2;options.log_severity_level=3
engine=ort.InferenceSession(str(ROOT/'app/src/main/assets/models/korean.onnx'),options,providers=['CPUExecutionProvider'])
chars=load(ROOT/'app/src/main/assets/models/characters.json')
views={r['file']:r['names'] for r in load(RAW/'menu-view-probe.json')}
baseline={r['file']:r for f in ['menus-app63.json','menus-app11.json'] for r in load(RAW/f)}
rows=[];pages={}
for case in cases():
    item=baseline[case['file']]['items'][case['index']]
    keys={key(item.get(k,'')) for k in ['name','originalName']}
    view=next((v for v in views.get(case['file'],[]) if key(v['old']) in keys),None)
    if not view:continue
    ml=next(v['text'] for v in view['views'] if v['scale']==1)
    if case['file'] not in pages:pages[case['file']]=cv2.cvtColor(np.array(ImageOps.exif_transpose(Image.open(case['source'])).convert('RGB')),cv2.COLOR_RGB2BGR)
    start=time.perf_counter();crop=patch(pages[case['file']],case['quad']);values,_=infer(engine,crop)
    tokens=aligned(values,chars)
    for t in tokens:
        row=values[t['first']];top=np.argsort(row)[-5:][::-1]
        t['score']=float(row[top[0]]);t['alternatives']=[dict(text=chars[j],score=float(row[j])) for j in top]
    selected,changes=choose(case['old'],tokens,ml)
    rows.append(dict(case,ml=ml,tokens=tokens,selected=selected,changes=changes,seconds=time.perf_counter()-start))
    if changes:print(case['file'],case['old'],'->',selected,flush=True)
(RAW/'region-cross-pc.json').write_text(json.dumps(dict(method='Stored Android ML + PC OpenCV crop Paddle emissions',rows=rows),ensure_ascii=False,indent=2,default=lambda v:v.item()),encoding='utf-8')
print(len(rows),'regions')
