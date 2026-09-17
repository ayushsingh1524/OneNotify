# Contributing during the testing phase

OneNotify is currently a local testing project. Hosting, paid services and real provider submissions are out of scope for this release. Use synthetic data and the documented demo accounts.

## Get started

```sh
git clone https://github.com/ayushsingh1524/OneNotify.git
cd OneNotify
docker compose up -d --build --wait
```

Open http://localhost:3000. [README.md](README.md) lists demo credentials and [docs/TESTING.md](docs/TESTING.md) explains the acceptance journey.

## Development

Use Java 21+, Maven 3.9+, Node 22 (see `.nvmrc`), Python 3.9+ and Docker Compose. For live-reload development, stop only the two app containers and retain infrastructure:

```sh
docker compose stop frontend backend
mvn -f services/backend/pom.xml spring-boot:run
# In a second terminal:
cd apps/web
npm ci
npm run dev
```

Do not run a host backend/frontend on ports already occupied by Docker. To switch back, stop those host processes and run `docker compose up -d --build --wait`.

## Before a pull request

1. Run `sh scripts/check.sh` for backend tests, formatting, frontend checks and build.
2. Install the browser once with `cd apps/web && npx playwright install chromium`.
3. Run `sh scripts/test-local.sh` from the repository root for full local integration, browser and isolated restore checks. It creates synthetic test records and leaves the application running.
4. Stage changes, then run `python3 scripts/check-repo.py` and `git diff --cached --check`.
5. Describe the user-visible change, checks run and any remaining limitations. Keep provider behavior explicitly simulated/manual unless a separately authorized integration exists.

Do not commit environment files, backup identities, archives, node_modules, build output, screenshots of real family data, reset links or access tokens. Example local credentials are intentionally public. Keep schema changes in new Flyway migrations; do not rewrite migrations already applied elsewhere.

No license grant is implied by publishing this source. A license has not been selected by the owner.
