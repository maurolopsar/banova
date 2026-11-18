# Networks (Velocity or BungeeCord with MySQL/MariaDB)

1. Create a MySQL or MariaDB database (for example `banova`) and a user.
2. Put the **same** `Banova.jar` on the proxy and on every backend.
3. In every `config.yml` (proxy: `plugins/banova/config.yml`, backends: `plugins/Banova/config.yml`):

```yaml
server-name: "lobby"      # different on each server: lobby, survival, proxy...
storage:
  type: mysql
  host: 127.0.0.1
  port: 3306
  database: banova
  username: banova
  password: "your password"
```

4. Restart everything.

## Who does what

| | Proxy (Velocity/BungeeCord) | Backends (Paper/Spigot/Folia) |
|---|---|---|
| Blocks banned players before they reach any server | ✔ | ✔ (backup) |
| Disconnects banned/kicked players from the whole network | ✔ | ✔ |
| Lockdown | ✔ | ✔ |
| Mutes and chat filter | | ✔ |
| Menus and staff commands | | ✔ |
| Web panel | optional | optional (enable on one server) |

Staff commands run on the backend the staff member is connected to; the result reaches every server in about one second (`network.sync-interval`) through a small table in the database. No Redis or plugin messaging is required and it also works while no players are online.

On the proxy the admin command is `/banovaproxy` (`/bproxy`): version, reload, web login approval and imports.

## BungeeCord

BungeeCord creates `plugins/Banova/config.yml`. Use the same database as the backends. The proxy checks bans during login and lockdown before connecting a player to a backend. The proxy admin command is `/banovaproxy`.
