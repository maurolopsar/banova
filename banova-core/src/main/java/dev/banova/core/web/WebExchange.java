package dev.banova.core.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience wrapper around {@link HttpExchange}.
 */
final class WebExchange {

    static final int MAX_BODY = 64 * 1024;

    private final HttpExchange exchange;
    private final boolean trustProxy;
    private Map<String, String> query;
    private JsonObject body;

    WebExchange(HttpExchange exchange, boolean trustProxy) {
        this.exchange = exchange;
        this.trustProxy = trustProxy;
    }

    String method() {
        return exchange.getRequestMethod().toUpperCase(java.util.Locale.ROOT);
    }

    String path() {
        String path = exchange.getRequestURI().getPath();
        return path == null || path.isEmpty() ? "/" : path;
    }

    String header(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    String ip() {
        if (trustProxy) {
            String forwarded = header("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String real = header("X-Real-IP");
            if (real != null && !real.isBlank()) {
                return real.trim();
            }
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    String userAgent() {
        String ua = header("User-Agent");
        return ua == null ? "" : ua;
    }

    String query(String key) {
        if (query == null) {
            query = new HashMap<>();
            String raw = exchange.getRequestURI().getRawQuery();
            if (raw != null) {
                for (String part : raw.split("&")) {
                    int eq = part.indexOf('=');
                    String k = eq < 0 ? part : part.substring(0, eq);
                    String v = eq < 0 ? "" : part.substring(eq + 1);
                    query.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
                }
            }
        }
        return query.get(key);
    }

    int queryInt(String key, int def) {
        try {
            String v = query(key);
            return v == null ? def : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    String cookie(String name) {
        String header = header("Cookie");
        if (header == null) {
            return null;
        }
        for (String part : header.split(";")) {
            String p = part.trim();
            if (p.startsWith(name + "=")) {
                return p.substring(name.length() + 1);
            }
        }
        return null;
    }

    /** JSON request body (empty object when missing or invalid). */
    JsonObject body() throws IOException {
        if (body != null) {
            return body;
        }
        try (InputStream in = exchange.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BODY) {
                    throw new IOException("Body too large");
                }
                out.write(buffer, 0, read);
            }
            String text = out.toString(StandardCharsets.UTF_8);
            JsonElement el = text.isBlank() ? null : JsonParser.parseString(text);
            body = el != null && el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException e) {
            body = new JsonObject();
        }
        return body;
    }

    String bodyString(String key) throws IOException {
        JsonElement el = body().get(key);
        return el == null || el.isJsonNull() ? null : el.getAsString();
    }

    boolean bodyBool(String key) throws IOException {
        JsonElement el = body().get(key);
        return el != null && !el.isJsonNull() && el.getAsBoolean();
    }

    Headers responseHeaders() {
        return exchange.getResponseHeaders();
    }

    void json(int status, JsonElement json) throws IOException {
        send(status, "application/json; charset=utf-8", json.toString().getBytes(StandardCharsets.UTF_8));
    }

    void error(int status, String code, String message) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("error", code);
        if (message != null) {
            o.addProperty("message", message);
        }
        json(status, o);
    }

    void send(int status, String contentType, byte[] data) throws IOException {
        Headers h = exchange.getResponseHeaders();
        h.set("Content-Type", contentType);
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("Referrer-Policy", "no-referrer");
        if (!h.containsKey("Cache-Control")) {
            h.set("Cache-Control", "no-store");
        }
        boolean head = method().equals("HEAD");
        exchange.sendResponseHeaders(status, head ? -1 : (data.length == 0 ? -1 : data.length));
        if (!head && data.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
        }
        exchange.close();
    }
}
