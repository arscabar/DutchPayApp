"""Diagnostic with manually selected line boxes; NOT an automatic accuracy test."""
import json
import math
from pathlib import Path
import numpy as np
import onnxruntime as ort
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    source=Path('C:/Users/surromind/Downloads/Document scans/Scan_20260520_191858.jpg')
    image=Image.open(source).convert('RGB');w,h=image.size
    region=image.crop((0,int(h*.35),int(w*.35),int(h*.44)))
    region.save(ROOT/'evidence/raw/scan-name-region.png')
    flat=region.rotate(-5,expand=True,fillcolor='white')
    assets=ROOT/'app/src/androidTest/assets/models'
    chars=json.loads((assets/'characters.json').read_text(encoding='utf8'))
    options=ort.SessionOptions();options.intra_op_num_threads=2
    options.log_severity_level=3
    session=ort.InferenceSession(str(assets/'korean.onnx'),options,providers=['CPUExecutionProvider'])
    result=[]
    for box in [(63,59,268,113),(64,111,225,164)]:
        crop=flat.crop(box);cw=math.ceil(crop.width*48/crop.height)
        pixels=np.asarray(crop.resize((cw,48),Image.Resampling.BILINEAR)).astype('float32')
        data=np.zeros((1,3,48,max(320,cw)),dtype='float32')
        data[0,:,:,:cw]=(pixels[:,:,::-1]/127.5-1).transpose(2,0,1)
        output=session.run(None,{'x':data})[0][0]
        assert output.shape[1]==len(chars)
        text=[];previous=-1
        for i in output.argmax(axis=1):
            if i and i!=previous:text.append(chars[i])
            previous=i
        result.append({'manual_box':box,'text':''.join(text)})
    (ROOT/'evidence/raw/manual-crop-diagnostic.json').write_text(
        json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps(result,ensure_ascii=True))
