"""PC CTC traces from actual Android crop pixels; no reference transcription."""
import json,time
import cv2,numpy as np,onnxruntime as ort
from region_data import RAW,ROOT,load
from menu_ctc_spans import infer,aligned

options=ort.SessionOptions();options.intra_op_num_threads=2;options.log_severity_level=3
model=ROOT/'app/src/main/assets/models/korean.onnx'
engine=ort.InferenceSession(str(model),options,providers=['CPUExecutionProvider'])
chars=load(model.with_name('characters.json'));out=RAW/'region-variant-trace.json'
done=load(out)['rows'] if out.exists() else [];seen={r['png'] for r in done}
for row in load(RAW/'region-cross-android.json')['rows']:
    if row['png'] in seen:continue
    crop=cv2.imdecode(np.fromfile(RAW/'region-probe'/row['png'],dtype=np.uint8),cv2.IMREAD_COLOR)
    h,w=crop.shape[:2];margin=max(1,int(h*.1+.5));start=time.perf_counter();views=[]
    for label,image in [('base',crop),('center80',crop[margin:h-margin])]:
        values,_=infer(engine,image);tokens=aligned(values,chars)
        for token in tokens:
            data=values[token['first']];top=np.argsort(-data,kind='stable')[:5]
            token['score']=float(data[top[0]])
            token['alternatives']=[dict(text=chars[k],score=float(data[k])) for k in top]
        views.append(dict(label=label,text=''.join(t['text'] for t in tokens),tokens=tokens))
    done.append(dict(file=row['file'],index=row['index'],old=row['old'],png=row['png'],
        views=views,seconds=time.perf_counter()-start))
    print(row['png'],flush=True)
out.write_text(json.dumps(dict(method='PC ONNX on unchanged Android PNG; OpenCV resize differs from Android',rows=done),ensure_ascii=False,indent=2,default=lambda v:v.item()),encoding='utf-8')
