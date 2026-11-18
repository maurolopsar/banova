package dev.banova.core.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and formats durations such as {@code 1d12h}, {@code 30m} or {@code perm}.
 */
public final class Durations {

    /** Marker for a permanent punishment. */
    public static final long PERMANENT = 0L;

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    private static final Set<String> PERMANENT_WORDS = Set.of(
            "perm", "permanent", "permanente", "forever", "siempre", "-1", "never", "nunca");

    private static final Pattern PART = Pattern.compile("(\\d+)\\s*(mo|min|y|w|d|h|m|s)");
    private static final Pattern FULL = Pattern.compile("(\\d+\\s*(mo|min|y|w|d|h|m|s))+");

    private Durations() {
    }

    /**
     * @return the duration in ms, {@link #PERMANENT} for permanent words, or {@code -1} when the text is not a duration
     */
    public static long parse(String input) {
        if (input == null) {
            return -1;
        }
        String text = input.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return -1;
        }
        if (PERMANENT_WORDS.contains(text)) {
            return PERMANENT;
        }
        if (!FULL.matcher(text).matches()) {
            return -1;
        }
        Matcher matcher = PART.matcher(text);
        long total = 0;
        while (matcher.find()) {
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
            long unit = switch (matcher.group(2)) {
                case "y" -> YEAR;
                case "mo" -> MONTH;
                case "w" -> WEEK;
                case "d" -> DAY;
                case "h" -> HOUR;
                case "m", "min" -> MINUTE;
                case "s" -> SECOND;
                default -> 0;
            };
            if (amount > Long.MAX_VALUE / Math.max(unit, 1) / 4) {
                return -1;
            }
            total += amount * unit;
        }
        return total <= 0 ? -1 : total;
    }

    /** True when the text looks like a duration (including permanent words). */
    public static boolean isDuration(String input) {
        return parse(input) >= 0;
    }

    /** Compact human format, e.g. {@code 2d 4h}. Shows at most two units. */
    public static String format(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        long[] units = {YEAR, MONTH, DAY, HOUR, MINUTE, SECOND};
        String[] names = {"y", "mo", "d", "h", "m", "s"};
        StringBuilder out = new StringBuilder();
        int shown = 0;
        long rest = millis;
        for (int i = 0; i < units.length && shown < 2; i++) {
            long amount = rest / units[i];
            if (amount > 0) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(amount).append(names[i]);
                rest -= amount * units[i];
                shown++;
            } else if (shown > 0) {
                break;
            }
        }
        if (out.length() == 0) {
            return "1s";
        }
        return out.toString();
    }

    /** Machine friendly format accepted by {@link #parse(String)}, e.g. {@code 1d12h}. */
    public static String toInput(long millis) {
        if (millis <= 0) {
            return "perm";
        }
        long[] units = {DAY, HOUR, MINUTE, SECOND};
        String[] names = {"d", "h", "m", "s"};
        StringBuilder out = new StringBuilder();
        long rest = millis;
        for (int i = 0; i < units.length; i++) {
            long amount = rest / units[i];
            if (amount > 0) {
                out.append(amount).append(names[i]);
                rest -= amount * units[i];
            }
        }
        return out.length() == 0 ? "1s" : out.toString();
    }
}
