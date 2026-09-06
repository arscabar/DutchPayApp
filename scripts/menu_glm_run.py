"""Local-only GLM-OCR full-page comparison; prompts never contain reference answers."""
import argparse
import json
import socket
import subprocess
import time
import urllib.request
from menu_glm_download import ROOT, DEST, digest
from menu_glm_input import inputs

def request(port, path, payload=None):
    body = None if payload is None else json.dumps(payload).encode()
    req = urllib.request.Request(f'http://127.0.0.1:{port}/{path}',data=body,
                                 headers={'Content-Type':'application/json'})
    return json.load(urllib.request.urlopen(req,timeout=180))

if __name__ == '__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('--crop',action='store_true')
    parser.add_argument('--table',action='store_true'); parser.add_argument('--f16',action='store_true')
    parser.add_argument('--limit',type=int,default=5); args=parser.parse_args()
    label='menu-glm'+('-f16' if args.f16 else '')+('-crop' if args.crop else '')+('-table' if args.table else '')
    with socket.socket() as sock:
        sock.bind(('127.0.0.1',0)); port=sock.getsockname()[1]
    command = [str(DEST/'bin/llama-server.exe'),'-m',str(DEST/('GLM-OCR-f16.gguf' if args.f16 else 'GLM-OCR-Q8_0.gguf')),
               '--mmproj',str(DEST/'mmproj-GLM-OCR-Q8_0.gguf'),'-ngl','99','-c','8192',
               '-np','1','-t','4','--host','127.0.0.1','--port',str(port)]
    raw=ROOT/'evidence/raw'; log=(raw/(label+'-server.log')).open('wb')
    start=time.perf_counter()
    process=subprocess.Popen(command,stdout=log,stderr=subprocess.STDOUT,
                              creationflags=subprocess.CREATE_NO_WINDOW)
    result={'command':command,'prompt':'Table Recognition:' if args.table else 'Text Recognition:',
            'temperature':0,'rows':[]}
    try:
        for attempt in range(120):
            if process.poll() is not None: raise RuntimeError('Server exited; inspect local log')
            try:
                if request(port,'health')['status']=='ok': break
            except Exception: pass
            time.sleep(1)
        else: raise TimeoutError('Server did not become ready')
        result['startupSeconds']=time.perf_counter()-start
        for file,pixels,box,payload in inputs(result['prompt'],args.crop,args.limit):
            start=time.perf_counter()
            response=request(port,'v1/chat/completions',payload)
            result['rows'].append({'file':file.name,'sourceSHA256':digest(file),'pixels':pixels,'automaticCrop':box,
                                  'seconds':time.perf_counter()-start,'response':response})
            (raw/(label+'-results.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
            print(file.name,round(result['rows'][-1]['seconds'],3),flush=True)
    finally:
        process.terminate()
        try: process.wait(timeout=10)
        except subprocess.TimeoutExpired: process.kill(); process.wait()
        log.close()
