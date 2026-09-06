"""Fetch pinned official artifacts and verify publisher-provided SHA256 values."""
import concurrent.futures
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT/'.tools/glm-ocr'
REV = '65a42de1148dbed2297e922b5dbc7d9b70c36578'
TAG = 'b10809'
get = lambda u: json.load(urllib.request.urlopen(u))

def digest(path):
    with path.open('rb') as stream: return hashlib.file_digest(stream,'sha256').hexdigest()

def fetch(spec):
    path = DEST/spec['name']
    if not path.exists() or digest(path) != spec['sha256']:
        temp = path.with_suffix(path.suffix+'.part')
        with urllib.request.urlopen(spec['url']) as remote, temp.open('wb') as local:
            while chunk := remote.read(4*1024*1024): local.write(chunk)
        assert temp.stat().st_size == spec['bytes']
        assert digest(temp) == spec['sha256'], 'Publisher hash mismatch'
        temp.replace(path)
    print('VERIFIED',spec['name'],path.stat().st_size,flush=True)
    if path.suffix == '.zip':
        target = (DEST/'bin').resolve(); target.mkdir(exist_ok=True)
        with zipfile.ZipFile(path) as archive:
            assert all((target/n).resolve().is_relative_to(target) for n in archive.namelist())
            archive.extractall(target)
    return spec

if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--f16',action='store_true');args=parser.parse_args()
    DEST.mkdir(parents=True,exist_ok=True)
    model = get('https://huggingface.co/api/models/ggml-org/GLM-OCR-GGUF/revision/'+REV+'?blobs=true')
    release = get('https://api.github.com/repos/ggml-org/llama.cpp/releases/tags/'+TAG)
    specs = []
    for obj in model['siblings']:
        if 'Q8_0.gguf' in obj['rfilename'] or (args.f16 and obj['rfilename']=='GLM-OCR-f16.gguf'):
            specs.append({'name':obj['rfilename'],'bytes':obj['size'],'sha256':obj['lfs']['sha256'],
                'url':'https://huggingface.co/ggml-org/GLM-OCR-GGUF/resolve/'+REV+'/'+obj['rfilename']})
    for obj in release['assets']:
        if 'win-cuda-12.4-x64.zip' in obj['name']:
            specs.append({'name':obj['name'],'bytes':obj['size'],'sha256':obj['digest'].split(':')[1],
                          'url':obj['browser_download_url']})
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool: results = list(pool.map(fetch,specs))
    (ROOT/'evidence/raw/menu-glm-artifacts.json').write_text(json.dumps({
        'modelRevision':REV,'llamaRelease':TAG,'artifacts':results},indent=2),encoding='utf-8')
