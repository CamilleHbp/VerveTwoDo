#!/usr/bin/env python3
"""Build and publish only the VerveTwoDo web service to the existing homeserver."""
from pathlib import Path
import datetime
import hashlib
import json
import os
import shutil
import subprocess
import tarfile
import sys

root = Path(__file__).resolve().parents[2]
pwa = root / 'pwa'
release = datetime.datetime.now(datetime.timezone.utc).strftime('1.0.0-web-%Y%m%d%H%M%S')
stage = root / 'build' / 'pwa' / release

def run(args, **kwargs):
    return subprocess.run(args, check=True, **kwargs)

if len(sys.argv) > 1:
    stage = Path(sys.argv[1]).resolve()
    release = stage.name
    assert stage.parent == (root / 'build' / 'pwa').resolve()
    for name, expected in json.loads((stage / 'verified-files.json').read_text()).items():
        path = stage / name
        assert path.stat().st_size == expected['size'] and hashlib.sha256(path.read_bytes()).hexdigest() == expected['sha256'], name
else:
    stage.mkdir(parents=True)
    run(['npm', 'run', 'build'], cwd=pwa)
    run(['npm', 'test'], cwd=pwa)
    run(['go', 'test', './...'], cwd=pwa / 'server', env=dict(os.environ, GOWORK='off'))
    run(['go', 'build', '-trimpath', '-o', str(stage / 'vervetwodo-web'), '.'], cwd=pwa / 'server',
        env=dict(os.environ, GOWORK='off', CGO_ENABLED='0', GOOS='linux', GOARCH='amd64'))
    shutil.copytree(pwa / 'dist', stage / 'dist')
    for name in ['Dockerfile', 'compose.yaml']:
        shutil.copy2(pwa / 'server' / name, stage / name)
    (stage / '.env').write_text('RELEASE_ID=' + release + '\n')
    files = {str(path.relative_to(stage)): {'size': path.stat().st_size, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
             for path in stage.rglob('*') if path.is_file()}
    (stage / 'verified-files.json').write_text(json.dumps(files, indent=2) + '\n')
archive = stage.parent / (release + '.tar.gz')
with tarfile.open(archive, 'w:gz') as out:
    for path in stage.iterdir():
        out.add(path, arcname=path.name)
remote = '/srv/homeserver/builds/vervetwodo-web/' + release
run(['ssh', 'nf-showcases', 'mkdir', '-p', remote])
run(['scp', str(archive), 'nf-showcases:' + remote + '/release.tar.gz'])
script = r'''
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tarfile
stage=Path(REMOTE)
with tarfile.open(stage/'release.tar.gz') as archive:
    # The homeserver's Python predates tarfile's extraction filters.
    for member in archive.getmembers():
        target=(stage/member.name).resolve()
        assert str(target).startswith(str(stage.resolve())+'/') and (member.isfile() or member.isdir()), member.name
    archive.extractall(stage)
for name, expected in json.loads((stage/'verified-files.json').read_text()).items():
    path=stage/name
    assert path.stat().st_size==expected['size'] and hashlib.sha256(path.read_bytes()).hexdigest()==expected['sha256'], name
root=Path('/home/debian/HomeServer')
data=Path('/srv/homeserver/data/vervetwodo-web')
if not data.exists():
    data.mkdir(mode=0o700)
    os.chown(data,10001,10001)
def run(*args,cwd=stage):subprocess.run(args,cwd=cwd,check=True)
current=stage.parent/'current'
previous=current.resolve() if current.exists() else None
caddy=root/'Caddyfile'
original=caddy.read_text()
domain='todo.camille.studio'
changed=False
try:
    run('docker','compose','-p','vervetwodo-web','up','-d','--build','--wait')
    if domain not in original:
        candidate=stage/'Caddyfile.candidate'
        candidate.write_text(original+'\n'+domain+' {\n    encode zstd gzip\n    reverse_proxy vervetwodo-web:8080\n}\n')
        run('docker','run','--rm','-v',str(candidate)+':/etc/caddy/Caddyfile:ro','caddy:2.10.2-alpine','caddy','validate','--config','/etc/caddy/Caddyfile','--adapter','caddyfile')
        shutil.copy2(caddy,stage/'Caddyfile.before')
        caddy.write_text(candidate.read_text());changed=True
        run('docker','compose','restart','caddy',cwd=root)
    temp=current.with_name('current-next')
    temp.unlink(missing_ok=True)
    temp.symlink_to(stage)
    temp.replace(current)
except BaseException:
    if changed:
        caddy.write_text(original)
        run('docker','compose','restart','caddy',cwd=root)
    if previous:
        run('docker','compose','-p','vervetwodo-web','up','-d','--wait',cwd=previous)
    raise
print('Verified web release deployed: '+stage.name)
'''.replace('REMOTE', repr(remote))
run(['ssh', 'nf-showcases', 'sudo', 'python3', '-'], input=script, text=True)
# Recheck bytes from public HTTPS, including the service worker and install icons.
run(['python3', str(pwa / 'scripts' / 'verify-public.py'), str(stage)])
print('https://todo.camille.studio')
