# GitHub Copilot instructions

Full policy: **[AGENTS.md](../AGENTS.md)**.

## 🔒 Most important rule: NEVER commit secrets
Never put real passwords, API keys, OAuth client secrets, JWT secrets, or AWS/payment keys
into committed files. Real values belong only in gitignored files
(`src/main/resources/application-local.yaml`, `.env`, `.env.production`) or environment
variables. Only `*.example` templates with placeholders are committed. Do not suggest
`git add -A`; stage explicit paths and review the diff for secrets first.
