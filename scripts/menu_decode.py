"""Dictionary-free greedy and bounded CTC prefix beam decoding."""
import math
import numpy as np


def decode(values, chars, beam_width=5):
    best = values.argmax(1)
    ids = [int(x) for i, x in enumerate(best) if x and (not i or x != best[i-1])]
    score = [float(values[i, x]) for i, x in enumerate(best) if x and (not i or x != best[i-1])]
    greedy = ''.join(chars[x] for x in ids)
    beams = {(): (0., -math.inf)}
    for row in values:
        top = np.argpartition(row, -8)[-8:]
        candidates = set(int(x) for x in top) | {0}
        logp = np.log(np.maximum(row, 1e-35))
        nxt = {}
        def add(prefix, blank=None, nonblank=None):
            b, n = nxt.get(prefix, (-math.inf, -math.inf))
            nxt[prefix] = (float(np.logaddexp(b, blank)) if blank is not None else b,
                           float(np.logaddexp(n, nonblank)) if nonblank is not None else n)
        for prefix, (b, n) in beams.items():
            total = float(np.logaddexp(b, n))
            add(prefix, blank=total+logp[0])
            for c in candidates-{0}:
                if prefix and prefix[-1] == c:
                    add(prefix, nonblank=n+logp[c])
                    add(prefix+(c,), nonblank=b+logp[c])
                else:
                    add(prefix+(c,), nonblank=total+logp[c])
        beams = dict(sorted(nxt.items(), key=lambda x: np.logaddexp(*x[1]), reverse=True)[:beam_width])
    winner = max(beams, key=lambda x: np.logaddexp(*beams[x]))
    return {'text': greedy, 'score': sum(score)/len(score) if score else 0.,
            'beam': ''.join(chars[x] for x in winner)}


if __name__ == '__main__':
    # Two identical symbols require an intervening blank in CTC.
    v = np.full((3, 9), 1e-8)
    v[0, 1] = v[1, 0] = v[2, 1] = 1
    assert decode(v, ['', 'a']+list('bcdefgh'))['beam'] == 'aa'
    v[1, 0], v[1, 1] = 1e-8, 1
    assert decode(v, ['', 'a']+list('bcdefgh'))['beam'] == 'a'
