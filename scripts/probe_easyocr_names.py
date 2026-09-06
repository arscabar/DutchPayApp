"""PC recognizer-only probe. Inputs: original image and automatic quads; no gold."""
import hashlib
import importlib.metadata as metadata
import json
import math
import time
from pathlib import Path

import cv2
import easyocr
import numpy as np
import torch
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
PROBE = ROOT / 'evidence/raw/name-probe-03.json'
OUTPUT = ROOT / 'evidence/raw/easy-name-03.json'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def crop(page, quad):
    q = np.floor(np.asarray(quad, np.float32) + .5)
    w = math.ceil(max(np.linalg.norm(q[1]-q[0]), np.linalg.norm(q[2]-q[3])))
    h = math.ceil(max(np.linalg.norm(q[3]-q[0]), np.linalg.norm(q[2]-q[1])))
    assert w >= 8 and h >= 16 and w*h <= 4_000_000
    dst = np.float32([[0, 0], [w, 0], [w, h], [0, h]])
    return cv2.warpPerspective(page, cv2.getPerspectiveTransform(q, dst), (w, h),
                               flags=cv2.INTER_LINEAR, borderValue=(255, 255, 255))


if __name__ == '__main__':
    data = json.loads(PROBE.read_text(encoding='utf-8-sig'))
    source = ROOT / 'app/src/androidTest/assets/receipts' / data['file']
    before = sha(source)
    page = cv2.cvtColor(np.array(ImageOps.exif_transpose(Image.open(source)).convert('RGB')),
                        cv2.COLOR_RGB2BGR)
    gpu = torch.cuda.is_available()
    torch.set_num_threads(2)
    start = time.perf_counter()
    reader = easyocr.Reader(['ko', 'en'], gpu=gpu, detector=False, quantize=False, verbose=False,
                           model_storage_directory=str(ROOT / '.tools/easyocr-model'))
    result = {'file': data['file'], 'sourceSHA256': before, 'probeSHA256': sha(PROBE),
              'device': torch.cuda.get_device_name() if gpu else 'CPU', 'cpuThreads': 2,
              'initIncludingDownloadSeconds': time.perf_counter()-start,
              'crop': 'Original automatic quad; OpenCV linear/white, approximates Android Canvas',
              'decoder': 'greedy', 'contrast_ths': 0, 'quantize': False,
              'versions': {p: metadata.version(p) for p in ['easyocr', 'torch', 'torchvision',
                           'numpy', 'opencv-python', 'scikit-image']}, 'rows': []}
    for i, row in enumerate(data['rows']):
        image = crop(page, row['quad'])
        if gpu: torch.cuda.synchronize()
        start = time.perf_counter()
        read = reader.recognize(cv2.cvtColor(image, cv2.COLOR_BGR2GRAY),
                                decoder='greedy', contrast_ths=0, paragraph=False)
        if gpu: torch.cuda.synchronize()
        result['rows'].append({'index': i, 'quad': row['quad'], 'size': list(image.shape[:2]),
                              'text': ' '.join(x[1] for x in read),
                              'scores': [float(x[2]) for x in read],
                              'seconds': time.perf_counter()-start})
    result['recognitionSeconds'] = sum(x['seconds'] for x in result['rows'])
    result['modelSHA256'] = sha(ROOT / '.tools/easyocr-model/korean_g2.pth')
    assert sha(source) == before and len(result['rows']) == len(data['rows'])
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f"{len(result['rows'])} rows; {result['device']}; {result['recognitionSeconds']:.3f}s")
