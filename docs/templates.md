# Templates and automatic escalation

Templates let staff punish consistently without remembering durations. Each template is a ladder of steps; Banova counts how many times the player was already punished with that template and applies the next step.

```yaml
templates:
  insults:
    name: "Insults / Toxicity"
    icon: SKELETON_SKULL        # item in the menu
    reason: "Insulting other players"
    reset-after: 60d            # older offenses do not count (perm = never reset)
    permission: ""              # optional permission to use this template
    silent: false               # optional: always silent
    steps:
      - { type: warn, duration: 7d }
      - { type: mute, duration: 1h }
      - { type: mute, duration: 1d }
      - { type: mute, duration: 7d }
```

- `type`: `ban`, `mute`, `warn` or `kick`. Add `ip: true` to a step to punish the IP too.
- After the last step, the last step repeats.
- **Removed punishments never count**, so an accepted appeal does not push the player up the ladder.
- The menu shows each template with the step the player would get next.

Use them:

- In game: `/punish <player>` and click a template (with a confirmation screen and a silent toggle).
- Command: `/punish <player> <template> [-s]`.
- Web panel: player profile > Quick punish, or New punishment > Template.

The chat filter also uses a template (`auto-punish.template` in `filter.yml`, `chat` by default) for players who keep breaking the chat rules.

On first install Banova writes the templates in the server language (English or Spanish). Edit them freely and run `/banova reload`.
