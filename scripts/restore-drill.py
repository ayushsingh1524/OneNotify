#!/usr/bin/env python3
"""Restore a logical DB snapshot into an isolated disposable PostgreSQL container.
No live database writes, no published port, no plaintext backup on disk.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import tarfile
import os
import subprocess
import time
import uuid

parser = argparse.ArgumentParser()
parser.add_argument('--encrypted-backup')
parser.add_argument('--identity')
args = parser.parse_args()
if bool(args.encrypted_backup) != bool(args.identity):
    raise SystemExit('Provide both --encrypted-backup and --identity')

name = 'onenotify-restore-' + uuid.uuid4().hex[:12]
compose = ['docker', 'compose']
if os.environ.get('COMPOSE_FILE'):
    raise SystemExit('Run this local synthetic-data drill with the default Compose project only.')

def run(args, **kwargs):
    return subprocess.run(args, check=True, capture_output=True, **kwargs).stdout

try:
    if args.encrypted_backup:
        folder = Path(args.encrypted_backup)
        manifest = json.loads((folder / 'manifest.json').read_text())
        for filename in ['database.dump.age', 'minio.tar.gz.age']:
            assert hashlib.sha256((folder / filename).read_bytes()).hexdigest() == manifest['files'][filename], 'Backup checksum mismatch'
        snapshot = run(['age', '-d', '-i', args.identity, str(folder / 'database.dump.age')])
        objects = run(['age', '-d', '-i', args.identity, str(folder / 'minio.tar.gz.age')])
        with tarfile.open(fileobj=io.BytesIO(objects), mode='r:gz') as archive:
            files = [m for m in archive.getmembers() if m.isfile()]
            assert files, 'Object archive is empty'
            for member in files:
                assert not member.name.startswith('/') and '..' not in Path(member.name).parts, 'Unsafe archive path'
                assert len(archive.extractfile(member).read()) == member.size, 'Truncated archive member'
        print('PASS: encrypted backup checksums, decryption and MinIO archive readability.')
    else:
        snapshot = run(compose + ['exec', '-T', 'postgres', 'sh', '-c',
            'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner --no-acl'])
    run(['docker', 'run', '-d', '--name', name, '--network', 'none',
         '--tmpfs', '/var/lib/postgresql/data', '-e', 'POSTGRES_HOST_AUTH_METHOD=trust', 'postgres:17-alpine'])
    for _ in range(60):
        ready = subprocess.run(['docker', 'exec', name, 'pg_isready', '-U', 'postgres'], capture_output=True)
        if ready.returncode == 0:
            break
        time.sleep(1)
    else:
        raise RuntimeError('Restore database did not become ready')
    run(['docker', 'exec', '-i', name, 'pg_restore', '-U', 'postgres', '-d', 'postgres',
         '--no-owner', '--no-acl', '--exit-on-error'], input=snapshot)
    query = "select count(*) from flyway_schema_history where success; select count(*) from information_schema.tables where table_schema='public'; select count(*) from pg_trigger where not tgisinternal;"
    values = run(['docker', 'exec', name, 'psql', '-U', 'postgres', '-d', 'postgres', '-At', '-c', query]).decode().splitlines()
    assert int(values[0]) >= 4 and int(values[1]) >= 25 and int(values[2]) >= 2, 'Schema or audit triggers missing'
    print('PASS: logical database snapshot restored into isolated PostgreSQL; migrations, tables and audit triggers present.')
    print('Scope: database restore only. Object-store recovery and key recovery still require an environment-specific drill.')
except subprocess.CalledProcessError as exc:
    raise SystemExit('Restore drill failed; command exit code ' + str(exc.returncode)) from None
finally:
    subprocess.run(['docker', 'rm', '-f', name], capture_output=True)
