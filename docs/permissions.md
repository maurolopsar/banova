# Permissions

## Groups (recommended)

| Node | Includes |
|---|---|
| `banova.helper` | Menu, templates (`banova.punish`), mute/tempmute, warn, kick, history, check, lists, notes (view/add), evidence, staff notifications, chat filter bypass. |
| `banova.moderator` | Everything in helper + ban/tempban, unban/unmute/unwarn, appeals, alts, staff history, web panel, alt/appeal/silent notifications. |
| `banova.admin` | Everything (IP bans, IPs, edit, audit, rollback, lockdown, import, reload, manage web sessions, bypass limits and exemptions). Default for OPs. |

Limit what a rank can do with **duration limits**: give `banova.limit.helper` or `banova.limit.moderator` (defined in `config.yml > punishments.limits`). While a limit applies, permanent punishments are not allowed.

## All nodes

| Node | What it allows |
|---|---|
| `banova.ban`, `banova.tempban`, `banova.ipban` | Bans |
| `banova.mute`, `banova.tempmute`, `banova.ipmute` | Mutes |
| `banova.warn`, `banova.kick` | Warnings and kicks |
| `banova.unban`, `banova.unmute`, `banova.unwarn` | Removing punishments |
| `banova.punish` | Templates / `/punish` |
| `banova.menu` | `/banova` main menu |
| `banova.history`, `banova.check`, `banova.list` | Looking up |
| `banova.alts` | Alt accounts |
| `banova.ips` | See full IP addresses (otherwise masked as 1.2.*.*) |
| `banova.notes`, `banova.notes.add`, `banova.notes.delete` | Staff notes |
| `banova.evidence`, `banova.evidence.delete` | Evidence |
| `banova.appeals` | Review appeals |
| `banova.appeal` | (Players) appeal in game — default true |
| `banova.edit` | Edit reason/duration |
| `banova.audit` | Audit log |
| `banova.staffhistory`, `banova.staffrollback` | Staff tools |
| `banova.lockdown`, `banova.lockdown.bypass` | Lockdown / join during a lockdown |
| `banova.import`, `banova.reload` | Administration |
| `banova.web`, `banova.web.admin` | Web panel / manage everyone's sessions |
| `banova.limit.<name>` | Apply a duration limit from `config.yml` |
| `banova.limit.bypass` | Ignore duration limits |
| `banova.exempt` | Cannot be punished |
| `banova.exempt.bypass` | Can punish exempt players |
| `banova.filter.bypass` | Not checked by the chat filter |
| `banova.notify` | Removal and lockdown notifications |
| `banova.notify.silent` | See silent punishments |
| `banova.notify.alts` | Ban evasion alerts |
| `banova.notify.appeals` | New appeal alerts |
| `banova.notify.filter` | Chat filter alerts |

Template permissions are set per template with `permission:` in `templates.yml`.

## Web panel permissions

When you log in, the panel copies your current Banova permissions. They are refreshed every time you join the game, and if you lose `banova.web` your sessions are closed.
