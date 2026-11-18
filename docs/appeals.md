# Appeals

Players can ask staff to remove a punishment. Staff accept (the punishment is removed and the audit log records why) or deny with a reply the player can read.

## How players appeal

- **Banned players** see an appeal code on the ban screen, for example `K7M2-QX9P`, and the address of the appeal page (`<web.public-url>/appeal`). They enter the code, read their punishment and write their appeal. With the same code they can come back later to read the reply. This needs the [web panel](web-panel.md) enabled.
- **Muted or warned players** write `/appeal <message>` in game, and `/appeal status` to see answers.
- If the web panel is disabled you can show your own link instead with `ban-screen.appeal-url` (Discord, forum...).

## How staff review

- Game: `/appeals` (menu) or `/appeals view|accept|deny <id> [reply]`. Staff with `banova.notify.appeals` get an alert when a new appeal arrives and a reminder when they join.
- Web panel: Appeals page, with the punishment, its evidence and previous appeals next to the message.
- Discord: new appeals can be posted to a channel (`discord.events.appeal`).

## Rules (`config.yml > appeals`)

| Option | Default | |
|---|---|---|
| `types` | `[ban, mute]` | What can be appealed. |
| `cooldown-after-deny` | `3d` | Wait before appealing again after a denial. |
| `max-per-punishment` | `3` | Maximum appeals per punishment. |
| `min-length` | `20` | Minimum characters. |

Only one appeal per punishment can be pending at a time, and the public page is rate limited per IP.
