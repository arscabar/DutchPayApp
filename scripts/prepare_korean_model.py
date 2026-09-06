"""Run with Python + PyYAML; downloads models only, never uploads receipts."""
import hashlib
import json
from pathlib import Path
from urllib.request import urlopen
import yaml

REV = '5c6f574b8e2230adf4287b33e736d71b9fabd28e'
BASE = f'https://huggingface.co/PaddlePaddle/korean_PP-OCRv5_mobile_rec_onnx/resolve/{REV}/'
ROOT = Path(__file__).resolve().parents[1]
DESTS = [ROOT / f'app/src/{source}/assets/models' for source in ('main', 'androidTest')]
SHA = '92f0b7785e64fc9090106a241cf4c1eb97472824558272751b88a2a4476d3a08'

if __name__ == '__main__':
    with urlopen(BASE + 'inference.onnx', timeout=120) as r:
        model = r.read()
    if hashlib.sha256(model).hexdigest() != SHA:
        raise ValueError('Model hash mismatch')
    with urlopen(BASE + 'inference.yml', timeout=30) as r:
        config = yaml.safe_load(r.read())
    chars = [''] + config['PostProcess']['character_dict'] + [' ']
    assert len(chars) == 11947
    for dest in DESTS:
        dest.mkdir(parents=True, exist_ok=True)
        (dest / 'korean.onnx').write_bytes(model)
        (dest / 'characters.json').write_text(json.dumps(chars, ensure_ascii=False), encoding='utf8')
    print('Verified official Korean model:', SHA)
