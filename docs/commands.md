# Commands

Durations: `30s`, `15m`, `2h`, `7d`, `2w`, `1mo`, `1y`, combinations like `1d12h`, or `perm`.
Add `-s` to any punishment to make it silent (only staff are told) or `-p` to force it public.
Every command also works from the console.

## Punishing

| Command | Description | Permission |
|---|---|---|
| `/punish <player>` | Opens the player menu (templates, custom punishments, history, notes, alts). Without arguments opens the main menu. | `banova.punish` |
| `/punish <player> <template> [-s]` | Applies the next escalation step of a template. | `banova.punish` (+ template permission) |
| `/ban <player> [duration] [reason]` | Ban. Without a duration it is permanent. | `banova.ban` (`banova.tempban` for temporary) |
| `/tempban <player> <duration> [reason]` | Temporary ban. | `banova.tempban` |
| `/ipban <player\|ip> [duration] [reason]` | Ban the IP (every account using it). | `banova.ipban` |
| `/mute`, `/tempmute`, `/ipmute` | Same as the ban commands, for chat. | `banova.mute`, `banova.tempmute`, `banova.ipmute` |
| `/warn <player> [duration] [reason]` | Warning (expires after `warnings.default-duration`). | `banova.warn` |
| `/kick <player> [reason]` | Kick. | `banova.kick` |
| `/unban <player\|ip\|#id> [reason]` | Remove a ban. | `banova.unban` |
| `/unmute <player\|ip\|#id> [reason]` | Remove a mute. | `banova.unmute` |
| `/unwarn <player\|#id> [reason]` | Remove the newest (or a specific) warning. | `banova.unwarn` |

## Looking up

| Command | Description | Permission |
|---|---|---|
| `/check <player>` | Ban/mute status, warnings, notes, alts, last seen. | `banova.check` |
| `/history <player> [page] [type]` | Punishment history (menu for players). | `banova.history` |
| `/banlist`, `/mutelist`, `/warnings` | Active punishments. | `banova.list` |
| `/alts <player>` | Accounts sharing an IP. | `banova.alts` |
| `/banova info <id>` | Details of a punishment. | `banova.history` |

## Notes and evidence

| Command | Description | Permission |
|---|---|---|
| `/note <player> <text>` | Add a private staff note. | `banova.notes.add` |
| `/notes <player>` | List notes. | `banova.notes` |
| `/notes delete <id>` | Delete a note (own notes always, others with permission). | `banova.notes.delete` |
| `/evidence <#id> <link or text>` | Attach evidence to a punishment. | `banova.evidence` |
| `/evidence <#id>` | List evidence. | `banova.evidence` |
| `/evidence remove <evidenceId>` | Remove evidence. | `banova.evidence.delete` |

## Appeals

| Command | Description | Permission |
|---|---|---|
| `/appeal <message>` | (Players) appeal an active mute or warning. `/appeal status` shows answers. | `banova.appeal` (default: everyone) |
| `/appeals` | Pending appeals (menu for players). | `banova.appeals` |
| `/appeals view <id>` | Read an appeal. | `banova.appeals` |
| `/appeals accept <id> [reply]` | Accept (removes the punishment). | `banova.appeals` |
| `/appeals deny <id> [reply]` | Deny. | `banova.appeals` |

## Staff and administration

| Command | Description | Permission |
|---|---|---|
| `/staffhistory <staff>` | Punishments made by a staff member (`console` for the console). | `banova.staffhistory` |
| `/staffrollback <staff> <time>` | Revoke everything a staff member did in that time (asks for confirmation). | `banova.staffrollback` |
| `/lockdown [on [reason] \| off]` | Close the server (network-wide) to non-staff. | `banova.lockdown` |
| `/banova` | Main menu (players) or help. | `banova.menu` |
| `/banova edit <id> reason <text>` | Change the reason. | `banova.edit` |
| `/banova edit <id> duration <duration>` | Change the duration (from the original date). | `banova.edit` |
| `/banova audit [staff] [page]` | Audit log. | `banova.audit` |
| `/banova import <source>` | Import from another plugin. | `banova.import` |
| `/banova web` | Web panel address and a one-click login link. | `banova.web` |
| `/banova web login <code>` | Approve a web panel login. | `banova.web` |
| `/banova web sessions` / `logout <id\|me\|all>` | Manage web sessions. | `banova.web` (`banova.web.admin` for everyone's) |
| `/banova reload` | Reload config, templates, filter and messages. | `banova.reload` |
| `/banova version` | Version, platform, storage and web status. | — |

On Velocity the admin command is `/banovaproxy` (alias `/bproxy`), so it does not hide the backend's `/banova` menu.
