#!/usr/bin/env python3
"""Check the Git index for private artifacts and common credentials; never print matches."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]

def git(*args):
    return subprocess.check_output(['git', '-C', str(root), *args])

paths = git('ls-files', '-z').decode().split('\0')
paths = [p for p in paths if p]
if not paths:
    raise SystemExit('No indexed files. Stage the intended source files first.')

patterns = [
    re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----'),
    re.compile(rb'AGE-SECRET-KEY-[0-9A-Z]{40,}'),
    re.compile(rb'gh[pousr]_[A-Za-z0-9]{30,}'),
    re.compile(rb'github_pat_[A-Za-z0-9_]{40,}'),
    re.compile(rb'AKIA[0-9A-Z]{16}'),
]

def env_values(path):
    values = {}
    if path.exists():
        for line in path.read_text().splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                key, value = line.split('=', 1)
                values[key.strip()] = value.strip().strip('"').strip("'")
    return values

examples = set(env_values(root / '.env.example').values())
secrets = set()
for filename in ['.env', '.env.staging']:
    for key, value in env_values(root / filename).items():
        if any(word in key for word in ['PASSWORD', 'SECRET', 'TOKEN', 'KEY']):
            if len(value) >= 16 and value not in examples and 'REPLACE_ME' not in value:
                secrets.add(value.encode())

errors = []
for name in paths:
    path = Path(name)
    parts = set(path.parts)
    private_env = path.name.startswith('.env') and path.name not in {'.env.example', '.env.staging.example'}
    if private_env or parts.intersection({'backups', 'node_modules', '.next', 'target', 'test-results', 'playwright-report', '__pycache__', '.idea', '.vscode'}):
        errors.append(name + ': private or generated artifact')
        continue
    if path.suffix in {'.pem', '.key', '.p12', '.pfx', '.age', '.tsbuildinfo', '.log', '.pyc'}:
        errors.append(name + ': excluded file type')
        continue
    data = git('show', ':' + name)
    if len(data) > 5 * 1024 * 1024:
        errors.append(name + ': unexpectedly large source artifact')
    if any(pattern.search(data) for pattern in patterns) or any(secret in data for secret in secrets):
        errors.append(name + ': possible credential detected (value withheld)')
if errors:
    raise SystemExit('\n'.join(errors))
print(f'PASS: {len(paths)} indexed files checked; no excluded artifacts or detected credentials. This is a focused check, not an exhaustive secret audit.')
