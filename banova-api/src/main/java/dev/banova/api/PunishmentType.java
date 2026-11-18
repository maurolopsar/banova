package dev.banova.api;

/**
 * Kinds of punishment Banova can apply.
 */
public enum PunishmentType {
    BAN,
    MUTE,
    WARN,
    KICK;

    /** Whether the punishment stays in effect over time (kicks are instant). */
    public boolean isLasting() {
        return this != KICK;
    }

    public static PunishmentType parse(String raw) {
        if (raw == null) {
            return null;
        }
        switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "ban":
            case "tempban":
            case "ipban":
                return BAN;
            case "mute":
            case "tempmute":
            case "ipmute":
                return MUTE;
            case "warn":
            case "warning":
                return WARN;
            case "kick":
                return KICK;
            default:
                return null;
        }
    }
}
