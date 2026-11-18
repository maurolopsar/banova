# Developer API

The `banova-api` module contains the public interfaces. Add Banova as a `softdepend` and use:

```java
import dev.banova.api.BanovaApi;
import dev.banova.api.BanovaListener;
import dev.banova.api.PunishmentView;

BanovaApi api = BanovaApi.get();

api.getActiveBan(player.getUniqueId()).thenAccept(ban -> {
    ban.ifPresent(b -> getLogger().info(b.getTargetName() + " is banned: " + b.getReason()));
});

api.registerListener(new BanovaListener() {
    @Override
    public void onPunish(PunishmentView p) {
        // called for punishments made on this server or anywhere on the network
    }

    @Override
    public void onRevoke(PunishmentView p) {
    }
});
```

All futures complete off the main thread, and listener methods are called asynchronously. Switch to the main thread before touching the Bukkit API.

Available methods: `getActiveBan`, `getActiveMute`, `getHistory`, `getPunishment`, `registerListener`, `unregisterListener`.
