"""Pinned official English recognizer; existing Python environments stay unchanged."""
import hashlib
import json
import argparse
from pathlib import Path
import urllib.request
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'.tools/english-model'
REPO='PaddlePaddle/en_PP-OCRv5_mobile_rec_onnx'
REV='3fafbc3b5dcf93dd72add9f48368be8a3a2cd33b'

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--install',action='store_true');args=parser.parse_args()
    DEST.mkdir(parents=True,exist_ok=True)
    url='https://huggingface.co/api/models/'+REPO+'/revision/'+REV+'?blobs=true'
    meta=json.load(urllib.request.urlopen(url)); artifacts=[]
    for source in meta['siblings']:
        name=source['rfilename']
        if name not in ['inference.onnx','inference.yml']:continue
        url='https://huggingface.co/'+REPO+'/resolve/'+REV+'/'+name
        data=urllib.request.urlopen(url).read(); sha=hashlib.sha256(data).hexdigest()
        if 'lfs' in source:assert sha==source['lfs']['sha256']
        else:assert hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()==source['blobId']
        (DEST/name).write_bytes(data)
        artifacts.append({'url':url,'bytes':len(data),'sha256':sha})
    record=ROOT/'evidence/raw/menu-english-artifacts.json'
    record.parent.mkdir(parents=True,exist_ok=True)
    record.write_text(json.dumps({
        'repository':REPO,'revision':REV,'artifacts':artifacts},indent=2),encoding='utf8')
    if args.install:
        import yaml
        target=ROOT/'app/src/main/assets/models';target.mkdir(parents=True,exist_ok=True)
        chars=['']+yaml.safe_load((DEST/'inference.yml').read_text(encoding='utf8'))['PostProcess']['character_dict']+[' ']
        assert len(chars)==438
        commit='2661c7c0ef5c613e8f93c6e93b2e052399f0f854'
        license=urllib.request.urlopen('https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/'+commit+'/LICENSE').read().decode()
        header=REPO+'\nRevision: '+REV+'\nhttps://huggingface.co/'+REPO+'\n\n'
        files={'english.onnx':(DEST/'inference.onnx').read_bytes(),
               'english-characters.json':json.dumps(chars,ensure_ascii=False).encode(),
               'english.LICENSE.txt':(header+license).encode()}
        for name,data in files.items():
            path=target/name
            if not path.exists() or path.read_bytes()!=data:path.write_bytes(data)
    print(artifacts)
