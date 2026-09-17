#!/usr/bin/env python3
"""Create private staging secret template without printing credentials or overwriting files."""
import base64
import os
from pathlib import Path
import secrets

root = Path(__file__).resolve().parents[1]
text = (root / '.env.staging.example').read_text()
values = {
    'POSTGRES_PASSWORD': secrets.token_urlsafe(36),
    'RUNTIME_DB_PASSWORD': secrets.token_urlsafe(36),
    'MINIO_ROOT_PASSWORD': secrets.token_urlsafe(36),
    'JWT_SECRET': secrets.token_urlsafe(64),
    'VAULT_KEY': base64.b64encode(secrets.token_bytes(32)).decode(),
}
for key, value in values.items():
    text = text.replace(f'{key}=REPLACE_ME', f'{key}={value}')
path = root / '.env.staging'
fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as out:
    out.write(text)
print('Created .env.staging with private permissions. Fill hostname, tester IP and SMTP settings; secrets were not printed.')
