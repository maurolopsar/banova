# Installation

## Single server (Paper / Spigot 1.18+)

1. Download `Banova-x.y.z.jar` and put it in `plugins/`.
2. Restart the server. Banova creates `plugins/Banova/` with:
   - `config.yml` – main settings
   - `templates.yml` – punishment templates with escalation
   - `filter.yml` – chat filter
   - `lang/en.yml`, `lang/es.yml` – every message
   - `data/` – the local H2 database
3. Give permissions to your staff (see [permissions](permissions.md)). The quickest way with LuckPerms:

```
/lp group helper permission set banova.helper true
/lp group mod permission set banova.moderator true
/lp group admin permission set banova.admin true
```

4. Done. Try `/punish <player>` in game.

New installations use English. Set `language: es` in `config.yml` and run `/banova reload` for Spanish.

## Updating

Replace the jar and restart. Your files are never overwritten; new options use their default value until you add them.

## Network

See [network.md](network.md).
