#!/usr/bin/env python3
"""Maintenance-window backup: stream DB and stopped MinIO volume through age encryption.
Requires age on PATH and AGE_RECIPIENT. Uses COMPOSE_FILE/COMPOSE_PROJECT_NAME if set.
Never archives .env or vault keys; escrow keys separately.
"""
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

recipient = os.environ.get('AGE_RECIPIENT', '')
if not recipient.startswith('age1') or not shutil.which('age'):
    raise SystemExit('Install age and set AGE_RECIPIENT to your backup public key before running.')
os.umask(0o077)
folder = Path('backups') / datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
folder.mkdir(parents=True, exist_ok=False, mode=0o700)
compose = ['docker', 'compose']
running = subprocess.check_output(compose + ['ps', '--services', '--status', 'running'], text=True).splitlines()
resume = [s for s in ['minio', 'backend'] if s in running]
if 'backend' not in running or 'minio' not in running:
    raise SystemExit('Backup expects a healthy running backend and MinIO; no services were stopped.')

def encrypted(args, filename):
    path = folder / filename
    producer = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    try:
        with path.open('xb') as out:
            result = subprocess.run(['age', '-r', recipient], stdin=producer.stdout, stdout=out, stderr=subprocess.DEVNULL)
        producer.stdout.close()
        if result.returncode or producer.wait():
            path.unlink(missing_ok=True)
            raise RuntimeError('Backup stream failed; no completed backup recorded')
    finally:
        if producer.poll() is None:
            producer.kill()
            producer.wait()

try:
    subprocess.run(compose + ['stop', 'backend', 'minio'], check=True)
    encrypted(compose + ['exec', '-T', 'postgres', 'sh', '-c',
        'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner --no-acl'], 'database.dump.age')
    storage_id = subprocess.check_output(compose + ['ps', '-aq', 'minio'], text=True).strip()
    if not storage_id: raise RuntimeError('Storage container missing')
    encrypted(['docker', 'run', '--rm', '--network', 'none', '--read-only', '--volumes-from', storage_id + ':ro', '--entrypoint', 'tar', 'postgres:17-alpine', '-C', '/data', '-czf', '-', '.'], 'minio.tar.gz.age')
    manifest = {'created_at': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'files': {}}
    for path in folder.glob('*.age'):
        digest = hashlib.sha256()
        with path.open('rb') as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b''): digest.update(chunk)
        manifest['files'][path.name] = digest.hexdigest()
    (folder / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('Encrypted backup completed in ' + str(folder) + '. Copy off-host and perform the recovery runbook.')
finally:
    subprocess.run(compose + ['start'] + resume, check=True)
