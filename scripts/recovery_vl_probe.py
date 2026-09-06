"""Local element-level VLM experiment on automatically detected menu quads."""
import argparse,json,re,time
from pathlib import Path
import cv2,numpy as np,torch
from PIL import Image,ImageOps
from transformers import AutoProcessor,AutoModelForImageTextToText
from menu_images import patch
from menu_probe import ROOT,RAW,load,sha
from evaluate_names import distance

def cases(limit):
    prior={r['file']:r for r in load(RAW/'menus-app63.json')}
    chosen={'Scan_20260806_190801.jpg','Scan_20260825_121832.jpg',
            'Scan_20260721_121352.jpg','Scan_20260723_124307.jpg'}
    key=lambda s:re.sub(r'[^A-Za-z0-9가-힣]','',s)
    for r in load(RAW/'document-scans-paddle-v6.json'):
        if r['file'] not in chosen:continue
        file=Path('C:/Users/surromind/Downloads/Document scans')/r['file']
        page=cv2.cvtColor(np.array(ImageOps.exif_transpose(Image.open(file)).convert('RGB')),cv2.COLOR_RGB2BGR)
        names=[key(i.get(k,'')) for i in prior[r['file']]['items'] for k in ('name','originalName') if i.get(k)]
        lines=[l for l in r['lines'] if len(re.findall('[가-힣]',l['text']))>=2
               and any(key(l['text']) in n or n in key(l['text']) or
                       distance(key(l['text']),n)/max(len(key(l['text'])),len(n),1)<.3 for n in names)]
        lines.sort(key=lambda l:min(p[1] for p in l['quad']))
        for l in lines[:limit]:yield file,page,l

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--limit',type=int,default=6);a=p.parse_args()
    processor=AutoProcessor.from_pretrained(ROOT/'.tools/vl-model')
    model=AutoModelForImageTextToText.from_pretrained(ROOT/'.tools/vl-model',dtype=torch.bfloat16).to('cuda').eval()
    rows=[]
    for file,page,line in cases(a.limit):
        crop=patch(page,line['quad']);h,w=crop.shape[:2]
        # Whitespace around one text region; no reconstructed or supplied characters.
        crop=cv2.copyMakeBorder(crop,max(8,h//4),max(8,h//4),8,8,cv2.BORDER_CONSTANT,value=(255,255,255))
        image=Image.fromarray(cv2.cvtColor(crop,cv2.COLOR_BGR2RGB));start=time.perf_counter()
        messages=[{'role':'user','content':[{'type':'image','image':image},{'type':'text','text':'OCR:'}]}]
        inputs=processor.apply_chat_template(messages,add_generation_prompt=True,tokenize=True,
            return_dict=True,return_tensors='pt',images_kwargs={'size':{'shortest_edge':112896,'longest_edge':1003520}}).to('cuda')
        with torch.inference_mode():out=model.generate(**inputs,max_new_tokens=192,do_sample=False,use_cache=True)
        tokens=out[0,inputs['input_ids'].shape[-1]:]
        row=dict(file=file.name,sourceSHA256=sha(file),old=line['text'],quad=line['quad'],
                 text=processor.decode(tokens,skip_special_tokens=True),tokens=len(tokens),
                 truncated=len(tokens)==192,seconds=time.perf_counter()-start)
        rows.append(row);print(json.dumps(row,ensure_ascii=False),flush=True)
        (RAW/'recovery-vl-regions.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
