# FAQ

**Does it work without a database?**
Yes. A single server uses a local H2 file in `plugins/Banova/data/`. MySQL/MariaDB is only needed for networks.

**Which versions are supported?**
Paper, Spigot, Folia, Velocity and BungeeCord. The Folia menu flow was exercised on 1.21.11 through BungeeCord build 2113.

**My /ban opens another plugin's command.**
Another plugin (for example EssentialsX) registered the same command. Use `/banova:ban`, or disable that command in the other plugin. EssentialsX normally gives way to other plugins automatically.

**The web panel does not open.**
Check `web.enabled`, that the port is open in your host's firewall/panel, and the console line `Web panel listening on ...`. On shared hosts you usually have to request an extra port.

**Players see "Not Secure" next to censored messages.**
That is Minecraft's chat signing: any plugin that changes a chat message (like censoring a word) removes the signature. Use `words.action: block` if you prefer to block those messages instead.

**Can I change the ban screen?**
Yes, `lang/<language>.yml` > `screen`.

**How do I reset someone's escalation?**
Remove their punishments for that template (`/unban`, `/unmute`, `/unwarn`) or wait for `reset-after`. Removed punishments do not count.

**Is my data safe if the database goes down?**
Banova never locks players out because of a database error: logins are allowed and the error is logged.
