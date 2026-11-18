package dev.banova.core.web;

import com.google.gson.*;
import dev.banova.core.BanovaCore;
import dev.banova.core.util.Durations;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Backs the panel's "Rules & settings" page. Edits templates.yml and a few config.yml keys in place,
 * keeping comments and the order of everything it does not touch.
 */
final class ConfigurationEditor {
    private static final Set<String> CATEGORIES = Set.of("chat", "gameplay", "account");
    private static final String ID = "[a-z0-9][a-z0-9_-]*";

    private final BanovaCore core;
    private final Yaml yaml;

    ConfigurationEditor(BanovaCore core) {
        this.core = core;
        LoaderOptions loader = new LoaderOptions();
        loader.setAllowDuplicateKeys(false);
        loader.setMaxAliasesForCollections(0);
        loader.setProcessComments(true);
        DumperOptions out = new DumperOptions();
        out.setProcessComments(true);
        out.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        // Same layout as the bundled files: "  list:\n    - item", long lines kept on one line.
        out.setIndent(2);
        out.setIndicatorIndent(2);
        out.setIndentWithIndicator(true);
        out.setWidth(4096);
        out.setSplitLines(false);
        yaml = new Yaml(new SafeConstructor(loader), new Representer(out), out, loader);
    }

    void handle(WebExchange ex, WebActor actor, String method) throws IOException {
        Path folder = core.platform().dataFolder();
        Path rules = folder.resolve("templates.yml");
        Path settings = folder.resolve("config.yml");
        if (method.equals("GET")) {
            String raw = Files.readString(rules);
            Map<?, ?> document = yaml.load(raw);
            JsonObject result = new JsonObject();
            JsonElement routes = new Gson().toJsonTree(document == null ? null : document.get("templates"));
            if (!routes.isJsonObject()) routes = new JsonObject();
            for (var template : core.templates().all()) {
                JsonElement rule = routes.getAsJsonObject().get(template.id());
                if (rule != null && rule.isJsonObject() && !rule.getAsJsonObject().has("category")) {
                    rule.getAsJsonObject().addProperty("category", template.category().key());
                }
            }
            result.add("routes", routes);
            result.addProperty("revision", revision(raw));
            result.addProperty("settingsRevision", revision(Files.readString(settings)));
            String filterRoute = core.filter().autoTemplate();
            if (filterRoute != null) result.addProperty("filterRoute", filterRoute);
            JsonObject values = new JsonObject();
            values.addProperty("language", core.config().getString("language", "en"));
            values.addProperty("broadcast", core.config().getString("punishments.broadcast", "all"));
            values.addProperty("silentByDefault", core.config().getBoolean("punishments.silent-by-default", false));
            values.addProperty("alertBanEvasion", core.config().getBoolean("punishments.alert-ban-evasion", true));
            values.addProperty("appealsEnabled", core.config().getBoolean("appeals.enabled", true));
            result.add("settings", values);
            ex.json(200, result);
            return;
        }
        if (!method.equals("POST")) { ex.error(405, "method", null); return; }
        try {
            JsonObject body = ex.body();
            String kind = field(body, "kind", 16);
            boolean onRules = switch (kind) {
                case "route", "layout", "delete" -> true;
                case "settings" -> false;
                default -> throw new IllegalArgumentException("kind");
            };
            Path path = onRules ? rules : settings;
            String raw = Files.readString(path);
            if (!revision(raw).equals(field(body, "revision", 64))) {
                ex.error(409, "configuration_changed", null); return;
            }
            MappingNode root = (MappingNode) yaml.compose(new StringReader(raw));
            String detail;
            switch (kind) {
                case "route" -> {
                    String id = id(body);
                    MappingNode routes = section(root, "templates");
                    put(routes, id, compact(yaml.represent(rule(body.getAsJsonObject("route")))));
                    detail = "route " + id;
                }
                case "layout" -> detail = layout(section(root, "templates"), body.getAsJsonArray("order"));
                case "delete" -> {
                    String id = id(body);
                    if (id.equals(core.filter().autoTemplate())) { ex.error(409, "route_in_use", null); return; }
                    MappingNode routes = section(root, "templates");
                    if (!routes.getValue().removeIf(t -> key(t).equals(id))) throw new IllegalArgumentException("id");
                    detail = "delete route " + id;
                }
                default -> {
                    JsonObject values = body.getAsJsonObject("settings");
                    String language = field(values, "language", 2);
                    String broadcast = field(values, "broadcast", 5);
                    if (!Set.of("en", "es").contains(language) || !Set.of("all", "staff", "none").contains(broadcast))
                        throw new IllegalArgumentException("settings");
                    put(root, "language", yaml.represent(language));
                    MappingNode punishments = section(root, "punishments");
                    put(punishments, "broadcast", yaml.represent(broadcast));
                    put(punishments, "silent-by-default", yaml.represent(bool(values, "silentByDefault")));
                    put(punishments, "alert-ban-evasion", yaml.represent(bool(values, "alertBanEvasion")));
                    put(section(root, "appeals"), "enabled", yaml.represent(bool(values, "appealsEnabled")));
                    detail = "settings";
                }
            }
            StringWriter out = new StringWriter();
            yaml.serialize(root, out);
            // Blank lines come back indented; also keep Windows line endings if the file had them.
            String text = out.toString().replaceAll("(?m)^[ \\t]+$", "")
                    .replaceAll("(?m)^([ \\t]*- )\\{([^{}\\n]*)\\}[ \\t]*$", "$1{ $2 }");
            if (raw.contains("\r\n")) text = text.replace("\r\n", "\n").replace("\n", "\r\n");
            yaml.load(text);
            Path temp = Files.createTempFile(folder, ".banova-", ".tmp");
            try {
                Files.writeString(temp, text);
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
            core.refreshRulesAndSettings();
            core.audit().log(actor.ref(), "CONFIG_EDIT", null, null, 0, detail);
            ex.json(200, new JsonObject());
        } catch (IllegalArgumentException | IllegalStateException | ClassCastException | NullPointerException
                 | UnsupportedOperationException e) {
            ex.error(400, "invalid_configuration", null);
        }
    }

    /**
     * Reorders the routes and moves them between menu rows. The order must list every route exactly once,
     * so a stale page can never drop a route. Only the "category" key changes inside each route.
     */
    private String layout(MappingNode routes, JsonArray order) {
        if (order == null || order.size() != routes.getValue().size()) throw new IllegalArgumentException("order");
        Map<String, NodeTuple> byId = new LinkedHashMap<>();
        for (NodeTuple tuple : routes.getValue()) byId.put(key(tuple), tuple);
        List<NodeTuple> sorted = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonElement entry : order) {
            JsonObject item = entry.getAsJsonObject();
            String id = field(item, "id", 64);
            String category = field(item, "category", 16);
            NodeTuple tuple = byId.get(id);
            if (tuple == null || !seen.add(id) || !CATEGORIES.contains(category)) throw new IllegalArgumentException("order");
            put((MappingNode) tuple.getValueNode(), "category", yaml.represent(category));
            sorted.add(tuple);
        }
        routes.getValue().clear();
        routes.getValue().addAll(sorted);
        return "route layout";
    }

    /** Writes steps as "- { type: warn, duration: 7d }" and aliases on one line, like the bundled file. */
    private static Node compact(Node route) {
        for (NodeTuple tuple : ((MappingNode) route).getValue()) {
            String name = key(tuple);
            if (name.equals("aliases") && tuple.getValueNode() instanceof SequenceNode list) {
                list.setFlowStyle(DumperOptions.FlowStyle.FLOW);
            } else if (name.equals("steps") && tuple.getValueNode() instanceof SequenceNode list) {
                for (Node step : list.getValue()) if (step instanceof MappingNode map) map.setFlowStyle(DumperOptions.FlowStyle.FLOW);
            }
        }
        return route;
    }

    private static String id(JsonObject body) {
        String id = field(body, "id", 64);
        if (!id.matches(ID)) throw new IllegalArgumentException("id");
        return id;
    }

    private static boolean bool(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException(name);
        return value.getAsBoolean();
    }

    private static String key(NodeTuple tuple) {
        return ((ScalarNode) tuple.getKeyNode()).getValue();
    }

    private static String revision(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private MappingNode section(MappingNode root, String key) {
        for (NodeTuple item : root.getValue()) {
            if (key(item).equals(key)) return (MappingNode) item.getValueNode();
        }
        MappingNode child = (MappingNode) yaml.represent(new LinkedHashMap<>());
        put(root, key, child);
        return child;
    }

    private void put(MappingNode map, String key, Node value) {
        ListIterator<NodeTuple> entries = map.getValue().listIterator();
        while (entries.hasNext()) {
            NodeTuple existing = entries.next();
            if (key(existing).equals(key)) {
                entries.set(new NodeTuple(existing.getKeyNode(), value));
                return;
            }
        }
        entries.add(new NodeTuple(yaml.represent(key), value));
    }

    private static String field(JsonObject object, String name, int max) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException(name);
        String result = value.getAsString().trim();
        if (result.isEmpty() || result.length() > max || result.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(name);
        return result;
    }

    private static String duration(JsonObject object, String key) {
        String value = field(object, key, 32);
        if (Durations.parse(value) < 0) throw new IllegalArgumentException(key);
        return value;
    }

    static Map<String,Object> rule(JsonObject input) {
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("name", field(input, "name", 100));
        if (input.has("category")) {
            String category = field(input, "category", 16);
            if (!CATEGORIES.contains(category)) throw new IllegalArgumentException("category");
            out.put("category", category);
        }
        if (input.has("icon")) {
            String icon = field(input, "icon", 80).toUpperCase(Locale.ROOT);
            if (!icon.matches("[A-Z0-9_]+")) throw new IllegalArgumentException("icon");
            out.put("icon", icon);
        }
        out.put("reason", field(input, "reason", 250));
        if (input.has("aliases")) {
            List<String> aliases = new ArrayList<>();
            for (var alias : input.getAsJsonArray("aliases")) {
                String value = alias.getAsString().trim().toLowerCase(Locale.ROOT);
                if (value.isBlank() || value.length() > 64 || value.contains(" ") || aliases.size() >= 64)
                    throw new IllegalArgumentException("aliases");
                if (!aliases.contains(value)) aliases.add(value);
            }
            if (!aliases.isEmpty()) out.put("aliases", aliases);
        }
        if (input.has("reset-after")) out.put("reset-after", duration(input, "reset-after"));
        if (input.has("permission") && !input.get("permission").getAsString().isBlank())
            out.put("permission", field(input, "permission", 100));
        out.put("silent", input.has("silent") && input.get("silent").getAsBoolean());
        JsonArray steps = input.getAsJsonArray("steps");
        if (steps == null || steps.isEmpty() || steps.size() > 64) throw new IllegalArgumentException("steps");
        List<Map<String,Object>> result = new ArrayList<>();
        for (JsonElement entry : steps) {
            JsonObject step = entry.getAsJsonObject();
            String type = field(step, "type", 4);
            if (!Set.of("ban","mute","kick","warn").contains(type)) throw new IllegalArgumentException("type");
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("type", type);
            if (!type.equals("kick")) item.put("duration", step.has("duration") ? duration(step, "duration") : "perm");
            if ((type.equals("ban") || type.equals("mute")) && step.has("ip") && step.get("ip").getAsBoolean()) item.put("ip", true);
            result.add(item);
        }
        out.put("steps", result);
        return out;
    }
}
