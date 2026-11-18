# Changelog

## 1.0.0 — first release

- Bans, temp-bans, IP bans, mutes, temp-mutes, IP mutes, warnings and kicks, with silent mode.
- Punishment templates with automatic escalation; warning thresholds.
- In-game menus for everything (`/punish`, `/banova`, history, notes, alts, evidence, appeals, audit).
- Appeals: public web page with codes on the ban screen, in-game `/appeal`, review from menus, commands or the panel.
- Private staff notes and evidence on punishments.
- Full audit log of staff actions.
- Chat filter: words (leetspeak, accents, spacing), advertising, spam, caps, flood, custom regex, automatic escalation.
- Ban evasion alerts and `/alts`.
- Duration limits per rank, staff history, staff rollback, lockdown, exemptions.
- Optional built-in web panel with code login (LuckPerms style) and persistent sessions; English and Spanish.
- Visual rule editor in the panel: drag-and-drop escalation steps, drag routes between the `/punish` menu rows, create and delete routes, server settings (language, announcements, silent by default, ban evasion alerts, appeals). Edits keep the comments and layout of `templates.yml` and `config.yml`.
- English by default everywhere (game, panel, appeal page); Spanish with `language: es`. Staff and players can switch the web pages between English and Spanish.
- Networks: Velocity support and MySQL/MariaDB sync without Redis.
- Discord webhooks.
- Importers: LiteBans, AdvancedBan, EssentialsX, vanilla.
- English and Spanish messages; public API.
