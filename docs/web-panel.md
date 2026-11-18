# Web panel

The panel is optional and built into the plugin: there is nothing else to install. Everything it does can also be done in game or from the console.

## Enable it

```yaml
web:
  enabled: true
  bind: 0.0.0.0          # 127.0.0.1 if you use a reverse proxy
  port: 8090
  public-url: "http://play.yourserver.com:8090"
```

Run `/banova reload` and open the `public-url`. Your host must allow that port (many hosts let you add an extra port in the control panel). In a network, enable it on **one** server only (any backend or the proxy).

## Rules and settings

Staff with `banova.config.edit` and panel access get the **Rules & settings** page. It has two tabs.

![Route editor](../publishing/screenshots/02-routes-editor.png)

**Punishment routes**

- The left board is the `/punish` menu: one row per case file row (Chat & conduct, Fair play, Account), in the order staff see them. The in-game menu shows 6 routes per row and page.
- **Drag a route** to reorder it or to move it to another row. The new order is saved at once. With the keyboard, focus a route and use the arrow keys (left/right to reorder, up/down to change row).
- **Click a route** to edit it on the right: name, reason shown to the player and the escalation.
- **Escalation:** drag *Warning*, *Mute*, *Ban* or *Kick* into the list, or click one to add it at the end. Drag the handle on a step to reorder it (or focus the handle and use the up/down arrows). Each step takes a duration (`30m`, `12h`, `7d`, `1mo`, `perm`) and, for bans and mutes, an optional IP punishment. After the last step, further offenses repeat it.
- **Forget old offenses after** sets `reset-after`: older offenses stop counting and the player starts again at the 1st step.
- **More options:** menu row, menu item (Minecraft material), ID, command aliases, required permission and "always silent".
- **New route** creates a route in that row. **Delete** removes it from `templates.yml`. Punishments already given keep their history. The route used by the chat filter's `auto-punish.template` cannot be deleted.

**Server settings:** language (`en` or `es`), who sees punishment announcements, silent-by-default, ban evasion alerts and appeals on/off.

Saving writes `templates.yml` or `config.yml` on the server running the panel and applies the change right away. The editor only rewrites what you changed, so comments and the rest of the file stay as they are. If the file was changed somewhere else (by hand or by another staff member) since you opened the page, Banova refuses the save and reloads the page instead of overwriting it. The page also warns you before you leave with unsaved changes. In a network, copy `templates.yml` to the other servers and run `/banova reload` there to offer the same routes everywhere.

## Language

The panel and the appeal page use the server language (`language` in `config.yml`, English by default). Each person can switch between English and Spanish with the EN/ES buttons, and the browser remembers that choice.

Press `/` to search for a player from any panel page.

## Logging in

Like LuckPerms' editor, without passwords:

1. Open the panel. It shows a code like `K7M2-QX9P` and the command to use.
2. Run `/banova web login K7M2-QX9P` in game or in any server console of your network.
3. The browser logs in by itself. Keep "Keep me logged in" checked to stay logged in for `session-days` (renewed while you use it).

In game you can also run `/banova web` and click the one-time link.

The command shows the browser and IP that asked for the code. If it was not you, run `/banova web logout all`.

## Security

- Sessions only keep the permissions you had when you logged in and are refreshed every time you join; losing `banova.web` closes them.
- Session tokens are random, stored hashed, sent as `HttpOnly; SameSite=Strict` cookies (and `Secure` when `public-url` is https).
- Every change requires an anti-CSRF header, login and appeal endpoints are rate limited, and the pages use a strict Content Security Policy.
- Manage sessions in the panel (Sessions) or with `/banova web sessions`.
- **Use HTTPS** if the panel is reachable from the internet: put it behind Caddy, nginx or a Cloudflare Tunnel, set `bind: 127.0.0.1`, `trust-proxy: true` and an `https://` `public-url`.

Example Caddyfile:

```
bans.yourserver.com {
    reverse_proxy 127.0.0.1:8090
}
```

## Appeal page

`<public-url>/appeal` is public (no login). See [appeals](appeals.md). Disable it with `web.appeals: false`.
