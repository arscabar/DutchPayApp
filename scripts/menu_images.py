"""Fixed, content-independent image variants; original image remains unchanged."""
import math
import cv2
import numpy as np


def patch(page, points, sdk=False):
    q = np.float32(points)
    if sdk:
        q = cv2.boxPoints(cv2.minAreaRect(q))
        p = sorted(q, key=lambda x: x[0]); a, d = sorted(p[:2], key=lambda x: x[1])
        b, c = sorted(p[2:], key=lambda x: x[1]); q = np.float32([a, b, c, d])
    else:
        q = np.floor(q+.5)
    rounder = int if sdk else math.ceil
    w = max(1, rounder(max(np.linalg.norm(q[1]-q[0]), np.linalg.norm(q[2]-q[3]))))
    h = max(1, rounder(max(np.linalg.norm(q[3]-q[0]), np.linalg.norm(q[2]-q[1]))))
    assert w*h < 4_000_000
    dst = np.float32([[0, 0], [w, 0], [w, h], [0, h]])
    out = cv2.warpPerspective(page, cv2.getPerspectiveTransform(q, dst), (w, h),
                             flags=cv2.INTER_CUBIC if sdk else cv2.INTER_LINEAR,
                             borderMode=cv2.BORDER_REPLICATE if sdk else cv2.BORDER_CONSTANT,
                             borderValue=(255, 255, 255))
    return cv2.rotate(out, cv2.ROTATE_90_COUNTERCLOCKWISE) if sdk and h/w >= 1.5 else out


def variants(page, points):
    q = patch(page, points); h, w = q.shape[:2]
    yield 'sdk', patch(page, points, True)
    yield 'quad', q
    yield 'unsharp', cv2.addWeighted(q, 1.7, cv2.GaussianBlur(q, (3, 3), 0), -.7, 0)
    gray = cv2.cvtColor(q, cv2.COLOR_BGR2GRAY)
    clahe = cv2.createCLAHE(clipLimit=2, tileGridSize=(8, 4)).apply(gray)
    yield 'clahe', cv2.cvtColor(clahe, cv2.COLOR_GRAY2BGR)
    binary = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY+cv2.THRESH_OTSU)[1]
    yield 'otsu', cv2.cvtColor(binary, cv2.COLOR_GRAY2BGR)
    for factor in (.8, 1.2):
        yield f'aspect{factor}', cv2.resize(q, (max(1, round(w*factor)), h))
    # Probe only: fixed central crop may clip true strokes; never auto-select by score.
    trim = max(1, round(h*.1))
    if h > 2*trim: yield 'center80', q[trim:h-trim].copy()
