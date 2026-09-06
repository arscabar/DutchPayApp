"""Pinned official detector only; keep the Korean v5 recognizer."""
import hashlib
from prepare_paddle_detector import ROOT, fetch

REV = '28fe5895c24fd108c19eb3e8479f4ab385fbfc62'
SHA = 'd73e0058b7a8086bbd57f3d10b8bcd4ff95363f67e06e2762b5e814fe9c9410e'

if __name__ == '__main__':
    model = fetch('https://huggingface.co/PaddlePaddle/PP-OCRv6_small_det_onnx/resolve/'
                  + REV + '/inference.onnx')
    if hashlib.sha256(model).hexdigest() != SHA:
        raise ValueError('Detector hash mismatch')
    for source in ('main', 'androidTest'):
        target = ROOT/f'app/src/{source}/assets/models/detector-v6.onnx'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(model)
    print('v6 small detector:', SHA)
