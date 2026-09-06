"""Replay selection only; this does not rerun Android OCR or read any gold labels."""
import hashlib
import json
from pathlib import Path
from menu_english_select import choose, compact

root = Path(__file__).resolve().parents[1]
source = root / 'evidence/raw/menu-english-android-before-number-guard.json'
if not source.exists():
    source.write_bytes((root / 'evidence/raw/menu-english-android.json').read_bytes())
rows = []
for receipt in json.loads(source.read_text(encoding='utf-8')):
    for item in receipt['names']:
        for observation in item['diagnostics']:
            variants = {str(i):dict(text=v['text'],score=v['score'],x=[v['left'],v['right']])
                        for i,v in enumerate(observation['views'])}
            old = observation['old']
            selected = choose(old,dict(variants=variants))
            if compact(old) == compact(selected): selected = old
            rows.append(dict(file=receipt['file'],itemOriginal=item['old'],oldSegment=old,
                             previousChoice=observation['new'],choice=selected,
                             accepted=selected != old,
                             blockedPrevious=observation['accepted'] and selected == old))
result = dict(method='Python selector replay of previously observed Android pixels; no OCR rerun',
              source=str(source.relative_to(root)),sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
              accepted=sum(r['accepted'] for r in rows),
              blockedPrevious=sum(r['blockedPrevious'] for r in rows),rows=rows)
out = root / 'evidence/raw/menu-english-android-guard-replay.json'
out.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))
