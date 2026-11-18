package dev.banova.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Small text helpers shared by every platform.
 */
public final class Text {

    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]{2,39}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_.*-]{1,16}$");
    private static final Pattern COLOR = Pattern.compile("(?i)[&§]([0-9a-fk-orx]|#[0-9a-f]{6})");

    private Text() {
    }

    /** Replaces {@code {key}} placeholders. */
    public static String replace(String text, Map<String, String> values) {
        if (text == null || values == null || values.isEmpty() || text.indexOf('{') < 0) {
            return text;
        }
        String out = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            out = out.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return out;
    }

    public static boolean isIp(String text) {
        if (text == null) {
            return false;
        }
        return IPV4.matcher(text).matches() || (text.contains(":") && IPV6.matcher(text).matches());
    }

    public static boolean isValidName(String text) {
        return text != null && NAME.matcher(text).matches();
    }

    public static UUID parseUuid(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        try {
            if (t.length() == 32) {
                t = t.substring(0, 8) + "-" + t.substring(8, 12) + "-" + t.substring(12, 16) + "-"
                        + t.substring(16, 20) + "-" + t.substring(20);
            }
            if (t.length() != 36) {
                return null;
            }
            return UUID.fromString(t);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Removes color codes. */
    public static String strip(String text) {
        return text == null ? "" : COLOR.matcher(text).replaceAll("");
    }

    /** Escapes color codes typed by users so they are shown literally. */
    public static String escapeColors(String text) {
        return text == null ? "" : text.replace("&", "&​").replace("§", "");
    }

    public static String join(String[] args, int from) {
        if (from >= args.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    /** Word-wraps text for item lore, keeping the given color prefix on each line. */
    public static List<String> wrap(String text, int width, String linePrefix) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (line.length() > 0 && line.length() + word.length() + 1 > width) {
                    lines.add(linePrefix + line);
                    line.setLength(0);
                }
                while (word.length() > width) {
                    lines.add(linePrefix + word.substring(0, width));
                    word = word.substring(width);
                }
                if (line.length() > 0) {
                    line.append(' ');
                }
                line.append(word);
            }
            if (line.length() > 0) {
                lines.add(linePrefix + line);
            }
        }
        return lines;
    }

    public static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…";
    }

    /** Masks an IP for staff without the IP permission: 192.168.*.*. */
    public static String maskIp(String ip) {
        if (ip == null) {
            return "";
        }
        int first = ip.indexOf('.');
        int second = first < 0 ? -1 : ip.indexOf('.', first + 1);
        if (second > 0) {
            return ip.substring(0, second) + ".*.*";
        }
        return "***";
    }
}
