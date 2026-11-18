package dev.banova.core.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only YAML document with dotted-path access and fallback to bundled defaults.
 * Files are never rewritten, so user comments are preserved.
 */
public final class YamlConfig {

    private final Map<String, Object> root;
    private final YamlConfig defaults;

    private YamlConfig(Map<String, Object> root, YamlConfig defaults) {
        this.root = root;
        this.defaults = defaults;
    }

    public static YamlConfig empty() {
        return new YamlConfig(new LinkedHashMap<>(), null);
    }

    public static YamlConfig load(Reader reader, YamlConfig defaults) {
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(50);
        Yaml yaml = new Yaml(new SafeConstructor(options), new Representer(new DumperOptions()), new DumperOptions(), options);
        Object loaded = yaml.load(reader);
        Map<String, Object> map = toStringMap(loaded);
        return new YamlConfig(map, defaults);
    }

    public static YamlConfig load(Path file, YamlConfig defaults) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return load(reader, defaults);
        }
    }

    public static YamlConfig loadResource(String resource) {
        try (InputStream in = YamlConfig.class.getResourceAsStream(resource)) {
            if (in == null) {
                return empty();
            }
            return load(new InputStreamReader(in, StandardCharsets.UTF_8), null);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bundled " + resource, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toStringMap(Object value) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object v = entry.getValue();
                if (v instanceof Map<?, ?>) {
                    v = toStringMap(v);
                }
                out.put(String.valueOf(entry.getKey()), v);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Object rawLocal(String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            Map<String, Object> section = (Map<String, Object>) current;
            current = section.get(part);
            // Older lang files used bare YAML 1.1 yes/no keys, parsed as true/false.
            if (current == null && (part.equals("yes") || part.equals("no"))) {
                current = section.get(part.equals("yes") ? "true" : "false");
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public Object get(String path) {
        Object value = rawLocal(path);
        if (value == null && defaults != null) {
            return defaults.get(path);
        }
        return value;
    }

    public boolean contains(String path) {
        return get(path) != null;
    }

    public String getString(String path, String def) {
        Object value = get(path);
        return value == null ? def : String.valueOf(value);
    }

    public String getString(String path) {
        return getString(path, "");
    }

    public int getInt(String path, int def) {
        Object value = get(path);
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return def;
    }

    public long getLong(String path, long def) {
        Object value = get(path);
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return def;
    }

    public double getDouble(String path, double def) {
        Object value = get(path);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return def;
    }

    public boolean getBoolean(String path, boolean def) {
        Object value = get(path);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value != null) {
            String s = String.valueOf(value).trim();
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("on")) {
                return true;
            }
            if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("off")) {
                return false;
            }
        }
        return def;
    }

    public List<String> getStringList(String path) {
        Object value = get(path);
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }
        if (value instanceof String s && !s.isEmpty()) {
            return List.of(s);
        }
        return Collections.emptyList();
    }

    /** Child keys of a section. Uses only the user's file when it defines the section. */
    @SuppressWarnings("unchecked")
    public Set<String> keys(String path) {
        Object value = path.isEmpty() ? root : rawLocal(path);
        if (value instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).keySet();
        }
        if (defaults != null) {
            return defaults.keys(path);
        }
        return Collections.emptySet();
    }

    /** A sub section as its own config (defaults follow the same path). */
    @SuppressWarnings("unchecked")
    public YamlConfig section(String path) {
        Object value = rawLocal(path);
        Map<String, Object> map = value instanceof Map<?, ?> m ? (Map<String, Object>) m : new LinkedHashMap<>();
        YamlConfig def = defaults == null ? null : defaults.section(path);
        return new YamlConfig(map, def);
    }

    /** List of maps (e.g. escalation steps). */
    @SuppressWarnings("unchecked")
    public List<YamlConfig> getSectionList(String path) {
        Object value = get(path);
        List<YamlConfig> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?>) {
                    out.add(new YamlConfig(toStringMap(o), null));
                }
            }
        }
        return out;
    }
}
