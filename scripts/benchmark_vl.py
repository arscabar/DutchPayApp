"""Local GPU OCR experiment; never reads reference answers or sends images."""
import json
import sys
import time
from pathlib import Path
import torch
from PIL import Image, ImageOps
from transformers import AutoProcessor, AutoModelForImageTextToText

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT/'.tools/vl-model'

if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf8')
    spotting = '--spotting' in sys.argv
    limit = 4096 if spotting else 2048
    assert torch.cuda.is_available(), 'CUDA runtime required'
    processor = AutoProcessor.from_pretrained(MODEL)
    model = AutoModelForImageTextToText.from_pretrained(
        MODEL, dtype=torch.bfloat16).to('cuda').eval()
    results = []
    for path in sorted((ROOT/'app/src/androidTest/assets/receipts').glob('*.jpg')):
        image = ImageOps.exif_transpose(Image.open(path)).convert('RGB')
        if spotting and max(image.size)<1500:
            image = image.resize((image.width*2,image.height*2),Image.Resampling.LANCZOS)
        start = time.perf_counter()
        messages = [{'role':'user', 'content':[
            {'type':'image', 'image':image},
            {'type':'text', 'text':'Spotting:' if spotting else 'OCR:'}]}]
        inputs = processor.apply_chat_template(messages, add_generation_prompt=True,
            tokenize=True, return_dict=True, return_tensors='pt',
            images_kwargs={'size':{'shortest_edge':112896,
                                  'longest_edge':(2048 if spotting else 1280)*28*28}}).to('cuda')
        with torch.inference_mode():
            output = model.generate(**inputs, max_new_tokens=limit, do_sample=False,
                                    use_cache=True)
        tokens = output[0, inputs['input_ids'].shape[-1]:]
        text = processor.decode(tokens, skip_special_tokens=True)
        result = {'file':path.name, 'text':text, 'tokens':len(tokens),
            'truncated':len(tokens)==limit,
            'seconds':time.perf_counter()-start}
        results.append(result)
        target = 'vl-spotting.json' if spotting else 'vl.json'
        (ROOT/'evidence/raw'/target).write_text(
            json.dumps(results,ensure_ascii=False,indent=2),encoding='utf8')
        print(path.name, round(result['seconds'],2), len(tokens), flush=True)
    assert len(results)==11 and not any(r['truncated'] for r in results)
