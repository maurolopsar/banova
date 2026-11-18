package dev.banova.core.config;

import dev.banova.core.util.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Localized messages. Lookup order: user's lang file, bundled file of the same language, bundled English.
 */
public final class Messages {

    private final YamlConfig lang;
    private final String code;

    public Messages(YamlConfig lang, String code) {
        this.lang = lang;
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * @param key           message key, e.g. {@code ban.success}
     * @param placeholders  alternating key/value pairs
     */
    public String get(String key, Object... placeholders) {
        String raw = raw(key);
        Map<String, String> values = new HashMap<>();
        values.put("prefix", rawOrEmpty("prefix"));
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            values.put(String.valueOf(placeholders[i]), placeholders[i + 1] == null ? "" : String.valueOf(placeholders[i + 1]));
        }
        // Placeholders can contain {prefix} only at the message level; replace twice is not needed.
        return Text.replace(raw, values);
    }

    /** Raw text without placeholders, or the key itself when missing (visible during development). */
    public String raw(String key) {
        Object value = lang.get(key);
        if (value == null) {
            return key;
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                sb.append(list.get(i));
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }

    private String rawOrEmpty(String key) {
        Object value = lang.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public boolean has(String key) {
        return lang.get(key) != null;
    }

    /** Raw list of lines (for item lore). */
    public List<String> lines(String key, Object... placeholders) {
        return List.of(get(key, placeholders).split("\n", -1));
    }
}
