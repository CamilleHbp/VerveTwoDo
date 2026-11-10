#!/usr/bin/env python3
from pathlib import Path
import hashlib
import json
import sys
import time
import urllib.request
import urllib.error
stage=Path(sys.argv[1])
base='https://todo.camille.studio'
files=json.loads((stage/'verified-files.json').read_text())
for attempt in range(12):
    try:
        urllib.request.urlopen(base+'/healthz',timeout=15).read()
        break
    except (OSError,urllib.error.URLError):
        if attempt==11:raise
        time.sleep(5)
count=0
for name,meta in files.items():
    if not name.startswith('dist/'):continue
    path=name.removeprefix('dist/')
    with urllib.request.urlopen(base+'/'+path,timeout=20) as response:
        contents=response.read()
        assert len(contents)==meta['size'],path
        assert hashlib.sha256(contents).hexdigest()==meta['sha256'],path
        if path=='sw.js':assert 'no-cache' in response.headers.get('Cache-Control','')
    count+=1
request=urllib.request.Request(base+'/api/state',data=b'{}',headers={'Origin':base,'Content-Type':'application/json'})
try:
    urllib.request.urlopen(request,timeout=15)
    raise AssertionError('Unauthenticated API access was accepted')
except urllib.error.HTTPError as error:
    assert error.code==401,error.code
print(f'Verified {count} public web files by size and SHA-256; private API requires authentication.')
