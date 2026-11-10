#!/usr/bin/env python3
"""Exercise two browser replicas against an isolated real VerveTwoDo sync server."""
from pathlib import Path
import http.cookiejar
import json
import os
import socket
import subprocess
import tempfile
import time
import urllib.request
import uuid
root=Path(__file__).resolve().parents[2]

def port():
    with socket.socket() as s:
        s.bind(('127.0.0.1',0));return s.getsockname()[1]

def wait(url):
    for _ in range(100):
        try:urllib.request.urlopen(url+'/healthz',timeout=1).read();return
        except OSError:time.sleep(.1)
    raise RuntimeError('Test server failed to start')

with tempfile.TemporaryDirectory(prefix='vervetwodo-web-sync-') as temp:
    directory=Path(temp);processes=[];logs=[]
    env=dict(os.environ,GOWORK='off')
    subprocess.run(['go','build','-o',str(directory/'sync'),'./cmd/server'],cwd=root/'sync-engine',env=env,check=True)
    subprocess.run(['go','build','-o',str(directory/'web'),'.'],cwd=root/'pwa/server',env=env,check=True)
    try:
        upstream=f'http://127.0.0.1:{port()}'
        (directory/'upstream').mkdir()
        def launch(binary,data,url,extra={}):
            log=open(directory/(data+'.log'),'w');logs.append(log)
            process=subprocess.Popen([str(directory/binary)],env=dict(env,DATA_DIR=str(directory/data),LISTEN_ADDR=url.removeprefix('http://'),**extra),stdout=log,stderr=log)
            processes.append(process);wait(url)
        launch('sync','upstream',upstream)
        code=json.loads((directory/'upstream/access-code.json').read_text())
        clients=[]
        for i in range(2):
            origin=f'http://127.0.0.1:{port()}'
            launch('web',f'web{i}',origin,{'SYNC_ORIGIN':upstream,'PUBLIC_ORIGIN':origin,'ASSET_DIR':str(root/'pwa/dist')})
            opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
            def call(method,body={},origin=origin,opener=opener):
                request=urllib.request.Request(origin+'/api/'+method,data=json.dumps(body).encode(),headers={'Origin':origin,'Content-Type':'application/json'})
                with opener.open(request,timeout=70) as r:return json.load(r)
            call('login',{'code':code});clients.append(call)
        a,b=clients
        identity=str(uuid.uuid4())
        def operation(field,value,replaces=[]):return {'id':str(uuid.uuid4()),'record':identity,'kind':'task','field':field,'value':value,'replaces':replaces}
        first=operation('title','Browser task')
        a('apply',{'operations':[first]});a('check');b('check')
        def record(reply):return next(r for r in reply['state']['records'] if r['id']==identity)
        assert record(b('state'))['fields']['title'][0]['value']=='Browser task'
        left=operation('title','First edit',[first['id']]);right=operation('title','Other edit',[first['id']]);details=operation('details','Independent notes')
        a('apply',{'operations':[left]});b('apply',{'operations':[right,details]})
        a('check');b('check');a('check')
        merged=record(a('state'))
        assert {v['value'] for v in merged['fields']['title']}=={'First edit','Other edit'}
        assert merged['fields']['details'][0]['value']=='Independent notes'
        chosen=operation('title','Resolved',[v['id'] for v in merged['fields']['title']])
        a('apply',{'operations':[chosen]});a('apply',{'operations':[chosen]})
        a('check');b('check')
        assert [v['value'] for v in record(b('state'))['fields']['title']]==['Resolved']
        a('logout')
        try:a('state');raise AssertionError('logout did not revoke access')
        except urllib.error.HTTPError as error:assert error.code==401
        print('PASS: pairing, two replicas, causal conflicts, independent edits, explicit resolution, replay, logout.')
    except BaseException:
        for log in logs:log.flush()
        # Logs are isolated test data; no production credentials or records are used.
        for path in directory.glob('*.log'):print(path.name, path.read_text()[-3000:])
        raise
    finally:
        for process in processes:process.terminate()
        for process in processes:
            try:process.wait(timeout=5)
            except subprocess.TimeoutExpired:process.kill();process.wait()
        for log in logs:log.close()
