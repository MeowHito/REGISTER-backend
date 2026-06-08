# CLAUDE.md

See **[AGENTS.md](./AGENTS.md)** for the full policy.

## 🔒 Most important rule: NEVER commit secrets
Real values (passwords, API keys, OAuth client secrets, JWT secrets, AWS/payment keys)
live **only** in gitignored files (`src/main/resources/application-local.yaml`, `.env`,
`.env.production`) or environment variables. Only `*.example` templates with placeholders
go in git. Review `git diff --staged` before committing; never `git add -A` blindly.
