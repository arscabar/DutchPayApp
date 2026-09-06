"""Run fixed crop variants on existing automatic menu quads, without gold strings."""
import hashlib
import json
import math
import re
import time
from pathlib import Path
import cv2
import numpy as np
import onnxruntime as ort
from PIL import Image, ImageOps
from menu_decode import decode
from menu_images import variants

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT/'evidence/raw'
load = lambda p: json.loads(p.read_text(encoding='utf-8-sig'))
compact = lambda s: re.sub(r'\s', '', s)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()


if __name__ == '__main__':
    chosen = set(load(RAW/'menu-files.json'))
    prior = {r['file']: r for r in load(RAW/'names-app63.json')}
    cases = []
    for receipt in load(RAW/'document-scans-paddle-v6.json'):
        if receipt['file'] not in chosen: continue
        names = [compact(i[k]) for i in prior[receipt['file']]['items']
                 for k in ['name', 'originalName'] if i.get(k)]
        selected = [l for l in receipt['lines'] if re.search('[가-힣]', l['text'])
                    and any(compact(l['text']) in n or n in compact(l['text']) for n in names)]
        cases.append((Path('C:/Users/surromind/Downloads/Document scans')/receipt['file'], selected))
    probe = load(RAW/'name-probe-03.json')
    cases.append((ROOT/'app/src/androidTest/assets/receipts/03.jpg', probe['rows']))
    assets = ROOT/'app/src/main/assets/models'
    chars = load(assets/'characters.json')
    options = ort.SessionOptions(); options.intra_op_num_threads = 2
    session = ort.InferenceSession(str(assets/'korean.onnx'), options, providers=['CPUExecutionProvider'])
    result = {'modelSHA256': sha(assets/'korean.onnx'), 'runtime': ort.__version__,
              'device': 'PC CPU; ORT intra-op 2', 'beamWidth': 5, 'beamTopK': 8, 'rows': []}
    for path, lines in cases:
        page = cv2.cvtColor(np.array(ImageOps.exif_transpose(Image.open(path)).convert('RGB')),
                            cv2.COLOR_RGB2BGR)
        for line in lines:
            row = {'file': path.name, 'sourceSHA256': sha(path), 'old': line['text'],
                   'quad': line['quad'], 'variants': {}}
            for mode, crop in variants(page, line['quad']):
                start = time.perf_counter(); h, w = crop.shape[:2]
                width = max(1, math.ceil(w*48/h))
                data = np.zeros((3, 48, max(320, width)), np.float32)
                data[:, :, :width] = cv2.resize(crop, (width, 48)).transpose(2, 0, 1)/127.5-1
                values = session.run(None, {'x': data[None]})[0][0]
                row['variants'][mode] = decode(values, chars)
                row['variants'][mode]['seconds'] = time.perf_counter()-start
            result['rows'].append(row)
        print(path.name, len(lines), flush=True)
        (RAW/'menu-pc-probe.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
