#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mvn -f services/backend/pom.xml test spotless:check
npm --prefix apps/web ci
npm --prefix apps/web run format:check
npm --prefix apps/web run lint
npm --prefix apps/web run typecheck
npm --prefix apps/web test
npm --prefix apps/web run build
printf '\nUnit/build checks passed. Run sh scripts/test-local.sh for integration and browser checks.\n'
