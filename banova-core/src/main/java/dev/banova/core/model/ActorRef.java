package dev.banova.core.model;

import java.util.UUID;

/**
 * Identifies who performed an action, for storage and auditing.
 */
public final class ActorRef {

    public static final ActorRef CONSOLE = new ActorRef(null, "Console", Source.CONSOLE);
    public static final ActorRef SYSTEM = new ActorRef(null, "Banova", Source.SYSTEM);

    private final UUID uuid;
    private final String name;
    private final Source source;

    public ActorRef(UUID uuid, String name, Source source) {
        this.uuid = uuid;
        this.name = name == null ? "Console" : name;
        this.source = source == null ? Source.CONSOLE : source;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public Source source() {
        return source;
    }

    public ActorRef withSource(Source newSource) {
        return new ActorRef(uuid, name, newSource);
    }
}
