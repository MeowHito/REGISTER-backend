# AGENTS.md — Rules for AI coding assistants

> This file applies to **every AI coding assistant** working in this repository —
> Claude Code, Cursor, GitHub Copilot, Windsurf, Cody, Gemini, Aider, and any other.
> `CLAUDE.md`, `.github/copilot-instructions.md`, and `.cursor/rules/` all point here.

---

## 🔒 RULE #1 — NEVER COMMIT SECRETS (highest priority, no exceptions)

Do **not** stage, commit, or push any secret or credential — ever. This includes:

- Passwords (database, mail, RabbitMQ, Redis, etc.)
- API keys / access tokens
- OAuth **client secrets** (e.g. `GOCSPX-…`), Google API keys (`AIza…`), AWS keys (`AKIA…`)
- JWT / signing secrets, private keys, `.pem`/`.key` files, certificates
- Payment provider keys (SCB, 2C2P)

### Files that must stay OUT of git (already gitignored — keep it that way)
- `src/main/resources/application-local.yaml`  ← real local secrets
- `.env`, `.env.production`  ← real environment secrets

Only the **`*.example` templates** belong in git, and they must contain
**placeholders only** (`<FILL>`, `dummy`, `localdev`) — never real values.

---

## ✅ How to add configuration the safe way
1. Put real values in **gitignored** files (`application-local.yaml`, `.env.production`) or env vars.
2. Production reads every secret from **environment variables** — see `application.yaml` (`${VAR}`).
3. When you add a new config key, add a **placeholder** entry to the matching `*.example` file so others know it exists.

## ✅ Before EVERY commit
- Review `git diff --staged` and look for anything secret-looking.
- **Never** run `git add -A` / `git add .` blindly — stage explicit paths.
- Never delete `.gitignore` entries that protect secret files.

## 🚨 If a secret gets committed by accident
1. STOP. Deleting it in a new commit is **not enough** — it stays in git history.
2. Treat it as compromised → **rotate/regenerate** it at the provider immediately.
3. Purge it from history: `git filter-repo --path <file> --invert-paths` then force-push.

---

## Note on `localdev`
`localdev` (in `docker-compose.yml`) is an **intentional local-only** MySQL password for the
dev container on `localhost` — it is not a real secret. Production uses values from the
gitignored `.env.production`.
