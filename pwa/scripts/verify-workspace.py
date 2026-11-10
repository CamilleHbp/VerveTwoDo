#!/usr/bin/env python3
"""Pair the web gateway and verify its server checkpoint without editing any tasks."""
import http.cookiejar
import json
import subprocess
import urllib.request

base = 'https://todo.camille.studio'
# Capture the existing owner code directly in memory; never log or export it.
code = json.loads(subprocess.check_output([
    'ssh', 'nf-showcases', 'sudo', 'cat',
    '/srv/homeserver/data/vervetwodo/access-code.json',
], text=True))
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

def call(method, body=None):
    request = urllib.request.Request(base + '/api/' + method,
        data=json.dumps(body or {}).encode(),
        headers={'Origin': base, 'Content-Type': 'application/json'})
    with opener.open(request, timeout=90) as response:
        return json.load(response)

call('login', {'code': code})
del code
try:
    state = call('check')
    assert state['state']['authenticated'], 'Gateway pairing failed'
    assert state['telemetry']['confirmed'], 'Gateway checkpoint not confirmed'
    records = state['state'].get('records') or []
    print('Production gateway paired and server checkpoint confirmed; '
          + str(len(records)) + ' records available. No task mutations performed.')
finally:
    call('logout')
