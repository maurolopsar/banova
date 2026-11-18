# Security policy

Banova includes an optional web panel and a public appeal page, so security reports are taken seriously.

## Reporting a problem

Please **do not open a public issue**. Use [GitHub's private vulnerability reporting](https://github.com/maurolopsar/banova/security/advisories/new) and include:

- the Banova version and server software,
- what an attacker can do and how (steps or a proof of concept),
- whether it needs the web panel enabled, a staff account or only a player account.

You will get an answer as soon as possible, and credit in the release notes if you want it.

## Supported versions

Only the latest release receives security fixes.

## Hardening tips

See [docs/web-panel.md](docs/web-panel.md#security): put the panel behind HTTPS, bind it to `127.0.0.1` when you use a reverse proxy, and review web sessions with `/banova web sessions`.
