#!/usr/bin/env python3
"""Validate merged staging config without printing secrets or creating resources."""
import argparse
import ipaddress
import json
import subprocess
import base64

parser = argparse.ArgumentParser()
parser.add_argument('--env-file', default='.env.staging')
parser.add_argument('--structure-only', action='store_true', help='Check template structure, not deployment readiness')
args = parser.parse_args()
r = subprocess.run(['docker', 'compose', '--env-file', args.env_file, '-p', 'onenotify-staging', '-f', 'compose.yaml', '-f', 'compose.staging.yaml', 'config', '--format', 'json'], capture_output=True, text=True)
if r.returncode:
    raise SystemExit('FAIL: Compose configuration could not resolve. Check required environment variable names.')
c = json.loads(r.stdout)
s = c['services']
errors = []
for name, service in s.items():
    if name != 'gateway' and service.get('ports'):
        errors.append(name + ' publishes a private service port')
b = s['backend']['environment']
if b['DEMO_ENABLED'] != 'false' or b['SECURE_COOKIE'] != 'true' or b['MALWARE_SCANNER'] != 'clamav':
    errors.append('Backend demo/cookie/scanner settings are unsafe')
if 'mailpit' in s['backend']['depends_on']:
    errors.append('Backend depends on the development mail sink')
if not args.structure_only:
    for key in ['JWT_SECRET', 'VAULT_KEY', 'POSTGRES_PASSWORD', 'MINIO_ROOT_PASSWORD', 'MAIL_HOST', 'MAIL_USERNAME', 'MAIL_PASSWORD', 'MAIL_FROM', 'APP_ORIGIN']:
        value = str(b.get(key, ''))
        if not value or 'REPLACE_ME' in value or 'example.' in value or 'local-' in value:
            errors.append(key + ' is not configured')
    if len(b['JWT_SECRET']) < 64:
        errors.append('JWT_SECRET must have at least 64 characters')
    try:
        if len(base64.b64decode(b['VAULT_KEY'], validate=True)) != 32: raise ValueError()
    except Exception:
        errors.append('VAULT_KEY must encode 32 random bytes')
    try:
        network = ipaddress.ip_network(s['gateway']['environment']['STAGING_ALLOWED_CIDR'], strict=False)
        if network.prefixlen == 0 or str(network.network_address).startswith('203.0.113.'):
            errors.append('Set a specific real tester public IP/CIDR')
    except ValueError:
        errors.append('Invalid tester IP/CIDR')
if errors:
    raise SystemExit('FAIL: ' + '; '.join(errors))
print('PASS: staging ' + ('template structure' if args.structure_only else 'configuration') + ' validated; no resources created.')
