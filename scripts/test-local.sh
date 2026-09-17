#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
# Start the local test stack; retain containers and test records for inspection.
docker compose up -d --build --wait
node scripts/integration.mjs
node scripts/security-extra.mjs
node scripts/manual-lifecycle.mjs
npm --prefix apps/web run test:e2e
python3 scripts/restore-drill.py
printf '\nLocal integration, browser and database restore checks passed. Services remain running.\n'
