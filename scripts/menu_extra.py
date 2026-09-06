"""Uniform follow-up crop probes. No gold, text replacement, or production changes."""
import math
import time
import cv2
import numpy as np
import onnxruntime as ort
from PIL import Image, ImageOps
from menu_probe import ROOT, RAW, load, sha
from menu_images import patch
from menu_decode import decode
import json

if __name__ == '__main__':
    assets = ROOT/'app/src/main/assets/models'
    chars = load(assets/'characters.json')
    options = ort.SessionOptions(); options.intra_op_num_threads = 2
    options.log_severity_level = 3
    session = ort.InferenceSession(str(assets/'korean.onnx'), options, providers=['CPUExecutionProvider'])
    data = load(RAW/'menu-pc-probe.json')
    out = {'modelSHA256': sha(assets/'korean.onnx'), 'device': 'PC CPU; ORT intra-op 2', 'rows': []}
    pages = {}
    for row in data['rows']:
        file = row['file']
        if file not in pages:
            path = (ROOT/'app/src/androidTest/assets/receipts/03.jpg' if file == '03.jpg'
                    else ROOT.__class__('C:/Users/surromind/Downloads/Document scans')/file)
            pages[file] = cv2.cvtColor(np.array(ImageOps.exif_transpose(Image.open(path)).convert('RGB')), cv2.COLOR_RGB2BGR)
        crop = patch(pages[file], row['quad']); h, w = crop.shape[:2]
        variants = {}
        for factor in (.6, .7):
            margin = round(h*(1-factor)/2)
            variants[f'center{round(factor*100)}'] = crop[margin:h-margin].copy()
        for factor in (.6, 1.4):
            variants[f'aspect{factor}'] = cv2.resize(crop, (max(1, round(w*factor)), h))
        item = {'file': file, 'old': row['old'], 'quad': row['quad'], 'variants': {}}
        for mode, image in variants.items():
            start = time.perf_counter(); ih, iw = image.shape[:2]
            width = max(1, math.ceil(iw*48/ih))
            tensor = np.zeros((3, 48, max(320, width)), np.float32)
            tensor[:, :, :width] = cv2.resize(image, (width, 48)).transpose(2, 0, 1)/127.5-1
            values = session.run(None, {'x': tensor[None]})[0][0]
            item['variants'][mode] = decode(values, chars)
            item['variants'][mode]['seconds'] = time.perf_counter()-start
        out['rows'].append(item)
    (RAW/'menu-pc-extra.json').write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding='utf-8')
    print(len(out['rows']), 'rows saved')
