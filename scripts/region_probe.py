"""CPU crop/window experiment on automatic app name regions, without reference labels."""
import argparse,json,time,hashlib,difflib
import cv2,numpy as np,onnxruntime as ort
from PIL import Image,ImageOps
from region_data import ROOT,RAW,load,cases
from region_windows import read,windows,choose
from menu_images import patch

def apply(old,tokens,edits):
    # Preserve corrections already made by the app; only map unchanged actual glyphs.
    source=''.join(t['text'] for t in tokens);mapping={};positions=[i for i,c in enumerate(old) if not c.isspace()]
    compact=''.join(old[i] for i in positions)
    for block in difflib.SequenceMatcher(None,source,compact,autojunk=False).get_matching_blocks():
        mapping.update({block.a+k:block.b+k for k in range(block.size)})
    if len(mapping)<len(source)*.8:return old
    chars=list(old)
    for i,c in edits.items():
        if i not in mapping:continue
        if i and mapping.get(i-1)!=mapping[i]-1:continue
        if i+1<len(tokens) and mapping.get(i+1)!=mapping[i]+1:continue
        chars[positions[mapping[i]]]=c
    return ''.join(chars)

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--files');ap.add_argument('--out',default='region-windows-v2.json');args=ap.parse_args()
    model=ROOT/'app/src/main/assets/models/korean.onnx';chars=load(model.with_name('characters.json'))
    options=ort.SessionOptions();options.intra_op_num_threads=2;options.log_severity_level=3
    session=ort.InferenceSession(str(model),options,providers=['CPUExecutionProvider'])
    result=dict(method='Automatic CTC overlapping windows; PC OpenCV linear crop, not Android Canvas',
                modelSHA256=hashlib.sha256(model.read_bytes()).hexdigest(),rows=[])
    pages={}
    for case in cases():
        if args.files and case['file'] not in args.files.split(','):continue
        file=case['file']
        if file not in pages:
            pages[file]=cv2.cvtColor(np.array(ImageOps.exif_transpose(Image.open(case['source'])).convert('RGB')),cv2.COLOR_RGB2BGR)
        start=time.perf_counter();crop=patch(pages[file],case['quad'])
        tokens,unit=read(session,chars,crop);observed=windows(session,chars,crop,tokens,unit)
        text,edits=choose(tokens,observed)
        gray=cv2.cvtColor(crop,cv2.COLOR_BGR2GRAY);_,ink=cv2.threshold(gray,0,255,cv2.THRESH_BINARY_INV+cv2.THRESH_OTSU)
        y,x=np.nonzero(ink);ih=0 if not len(y) else int(y.max()-y.min()+1)
        row=dict(case,shape=list(crop.shape[:2]),effectiveInkHeight48=48*ih/crop.shape[0],
                 tokens=tokens,windows=observed,candidate=text,edits=edits,
                 selected=apply(case['old'],tokens,edits),seconds=time.perf_counter()-start)
        result['rows'].append(row)
        if row['old']!=row['selected']:print(file,repr(row['old']),'->',repr(row['selected']),flush=True)
    (RAW/args.out).write_text(json.dumps(result,ensure_ascii=False,indent=2,default=lambda v:v.item()),encoding='utf-8')
    print(len(result['rows']),'regions',sum(len(r['windows']) for r in result['rows']),'windows',flush=True)
