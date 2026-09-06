"""Prepare the required app SDK and legacy v5 test detector; no receipt upload."""
import hashlib
import json
from pathlib import Path
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[1]
REV = '2661c7c0ef5c613e8f93c6e93b2e052399f0f854'
MODEL_REV = 'e6f4fa85f00e168c862bc462aebca69eef9b3d3d'
PREFIX = 'deploy/ppocr-android/ppocr-sdk/'

def fetch(url):
    with urlopen(url, timeout=120) as r:
        return r.read()

if __name__ == '__main__':
    dest = ROOT/'.tools/paddle-sdk'
    tree = json.loads(fetch(f'https://api.github.com/repos/PaddlePaddle/PaddleOCR/git/trees/{REV}?recursive=1'))
    files = [x['path'] for x in tree['tree'] if x['path'].startswith(PREFIX+'src/main/') and x['type']=='blob']
    for name in files:
        path = dest/name.removeprefix(PREFIX)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(fetch(f'https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/{REV}/{name}'))
    (dest/'LICENSE').write_bytes(fetch(f'https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/{REV}/LICENSE'))
    (dest/'build.gradle').write_bytes((ROOT/'experiments/paddle-sdk.gradle').read_bytes())
    model = fetch('https://huggingface.co/PaddlePaddle/PP-OCRv5_mobile_det_onnx/resolve/'+MODEL_REV+'/inference.onnx')
    if hashlib.sha256(model).hexdigest() != 'a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d':
        raise ValueError('Detector hash mismatch')
    target = ROOT/'app/src/androidTest/assets/models/detector.onnx'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(model)
    print('SDK revision:', REV, 'files:', len(files))
    print('Detector SHA256:', hashlib.sha256(model).hexdigest())
