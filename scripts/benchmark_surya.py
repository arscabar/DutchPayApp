"""Surya 2 full-page prompt, local Transformers GPU experiment."""
import json
import argparse
import time
from pathlib import Path
import torch
from PIL import Image, ImageOps
from transformers import AutoProcessor, AutoModelForImageTextToText

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT/'.tools/surya-model'
PROMPT = ('OCR this image to HTML. Each block is a div with data-label and data-bbox '
          '(x0 y0 x1 y1, normalized 0-1000).')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input',type=Path,default=ROOT/'app/src/androidTest/assets/receipts')
    parser.add_argument('--output',type=Path,default=ROOT/'evidence/raw/surya.json')
    args = parser.parse_args()
    paths = sorted(args.input.glob('*.jpg'))
    assert paths, 'No JPEG inputs'
    if args.output.exists() and any(args.output.samefile(p) for p in paths):
        raise ValueError('Output must not overwrite an input image')
    assert torch.cuda.is_available()
    processor = AutoProcessor.from_pretrained(MODEL)
    model = AutoModelForImageTextToText.from_pretrained(
        MODEL, dtype=torch.bfloat16).to('cuda').eval()
    results = []
    for path in paths:
        image = ImageOps.exif_transpose(Image.open(path)).convert('RGB')
        start = time.perf_counter()
        inputs = processor.apply_chat_template([{'role':'user','content':[
            {'type':'image','image':image}, {'type':'text','text':PROMPT}]}],
            add_generation_prompt=True, tokenize=True, return_dict=True,
            return_tensors='pt', processor_kwargs={'images_kwargs':{
                'size':{'shortest_edge':65536,'longest_edge':1605632}}}).to('cuda')
        with torch.inference_mode():
            # ponytail: blocks long exact repeats; may also block valid repeated rows.
            output = model.generate(**inputs,max_new_tokens=4096,
                do_sample=False,use_cache=True,no_repeat_ngram_size=32)
        tokens = output[0,inputs['input_ids'].shape[-1]:]
        results.append({'file':path.name,
            'text':processor.decode(tokens,skip_special_tokens=True),
            'tokens':len(tokens),'truncated':len(tokens)==4096,
            'seconds':time.perf_counter()-start})
        args.output.write_text(
            json.dumps(results,ensure_ascii=False,indent=2),encoding='utf8')
        print(path.name,round(results[-1]['seconds'],2),len(tokens),flush=True)
    assert len(results)==len(paths) and not any(r['truncated'] for r in results)
