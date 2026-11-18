# Chat filter

Configured in `filter.yml`. Staff with `banova.filter.bypass` are never filtered; staff with `banova.notify.filter` see what was filtered.

| Section | What it does |
|---|---|
| `words` | Blocked words. `action: censor` replaces them with `****`, `block` drops the message. Detects upper/lower case, accents, leetspeak (`1d10t4`), repeated letters (`idiooota`) and letters separated by spaces (`i d i o t a`, which is always blocked). `whitelist` lists words that must never be flagged. |
| `advertising` | IP addresses and domains (also written as `server dot com`). `whitelist` contains allowed domains. |
| `spam` | Messages sent faster than `min-delay-ms`, or too similar (`similarity`) to one of the last `history` messages. |
| `caps` | Messages with more than `max-percent` capital letters are lowered (player names are kept). |
| `flood` | Long runs of the same character (`hellooooooo`) are shortened. |
| `custom` | Your own regular expressions, each with `block` or `censor`. A Discord invite rule is included. |
| `auto-punish` | After `violations` filtered messages within `window`, the `template` is applied automatically (with escalation). |

Mutes are checked before the filter: muted players cannot talk or use the commands in `punishments.muted-blocked-commands`.
