# Configuration

All files live in `plugins/Banova/` (`plugins/banova/` on Velocity). Every option is commented in the file itself; this page explains the important ones. Apply changes with `/banova reload` (storage changes need a restart).

## config.yml

| Option | Default | Meaning |
|---|---|---|
| `language` | `en` | Message file from `lang/`. Set `es` for Spanish. |
| `server-name` | `server` | Shown on punishments; use a different one on each server of a network. |
| `storage.type` | `h2` | `h2` = local file (single server). `mysql` = MySQL or MariaDB (required for networks). |
| `storage.host/port/database/username/password` | | MySQL/MariaDB connection. |
| `storage.table-prefix` | `banova_` | Table name prefix. |
| `network.sync-interval` | `1000` | How often (ms) servers check for punishments made elsewhere. |
| `punishments.broadcast` | `all` | Who sees announcements: `all`, `staff` or `none`. |
| `punishments.silent-by-default` | `false` | Make every punishment silent unless `-p` is used. |
| `punishments.replace-existing` | `true` | A new ban/mute replaces the active one instead of failing. |
| `punishments.alert-ban-evasion` | `true` | Alert staff when a player shares an IP with a banned account. |
| `punishments.unknown-players` | `auto` | How players that never joined are resolved: `auto`, `mojang`, `offline`, `deny`. |
| `punishments.muted-blocked-commands` | msg, tell, r... | Commands muted players cannot use. |
| `punishments.limits` | helper/moderator | Max durations per rank (`banova.limit.<name>`). |
| `warnings.default-duration` | `30d` | Lifetime of warnings without a duration. |
| `warnings.thresholds` | 3 → mute 1h, 5 → ban 1d | Automatic punishment at N active warnings. |
| `appeals.*` | | Which types can be appealed, cooldown after a denial, max appeals, minimum length. |
| `ban-screen.appeal-url` | empty | Link shown on the ban screen when the web panel is disabled (Discord, forum...). |
| `web.*` | disabled | See [web panel](web-panel.md). |
| `discord.*` | disabled | Webhook URL and which events to send. |
| `date-format`, `timezone` | | How dates are shown. |

## Messages (`lang/*.yml`)

Every text players and staff see, including the ban screen (`screen.ban-permanent`, `screen.ban-temporary`), the mute message and menus. Colors use `&` codes and `&#rrggbb` hex.

Placeholders available in punishment messages:

`{id} {player} {reason} {actor} {type} {date} {expires} {remaining} {duration} {server} {status} {appeal_code} {appeal} {ip} {silent} {revoked_by} {revoke_reason}`

`{appeal}` turns into the "appeal at ... with code ..." line, or disappears when appeals are not available.

## templates.yml and filter.yml

See [templates](templates.md) and [chat filter](filter.md).
