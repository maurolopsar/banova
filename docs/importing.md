# Importing from other plugins

```
/banova import <source>            # shows what was found, changes nothing
/banova import <source> confirm    # imports
```

Imports are safe to repeat: anything already imported is skipped. Active bans and mutes keep working right away, and the audit log records the import.

| Source | Reads | Notes |
|---|---|---|
| `litebans` | LiteBans MySQL/MariaDB tables (connection read from `plugins/LiteBans/config.yml`) | Bans, mutes, warnings, kicks, removals, silent flag, server, player names. LiteBans' local H2 file cannot be read: switch LiteBans to MySQL first (LiteBans can migrate its own data). |
| `advancedban` | AdvancedBan MySQL tables (`plugins/AdvancedBan/MySQL.yml`) | Punishments, history and **notes** (become Banova notes). Local HSQLDB storage is not supported. |
| `essentials` | `banned-players.json` + `plugins/Essentials/userdata/*.yml` | Bans and active mutes. |
| `vanilla` | `banned-players.json` and `banned-ips.json` | Minecraft's own ban list. |

If the other plugin's config cannot be found (for example you already removed it), fill the `import:` section of Banova's `config.yml` with the database details.

Run imports on the server where the other plugin was installed. After importing you can remove the old plugin.
