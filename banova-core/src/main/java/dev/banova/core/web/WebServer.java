package dev.banova.core.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.banova.core.BanovaCore;
import dev.banova.core.config.YamlConfig;
import dev.banova.core.model.Records.WebSession;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.service.TemplateService;
import dev.banova.core.util.Codes;
import dev.banova.core.util.Durations;
import dev.banova.core.util.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Optional built-in web panel (JDK HTTP server, no extra dependencies).
 *
 * <p>Login works like LuckPerms' editor: the browser shows a code, staff type
 * {@code /banova web login CODE} in game or console, and the browser is signed in.
 * Pending logins live in the database, so the command works on any server of a network.</p>
 */
public final class WebServer {

    static final String COOKIE = "banova_session";
    private static final long CODE_TTL = TimeUnit.MINUTES.toMillis(5);
    private static final long LINK_TTL = TimeUnit.MINUTES.toMillis(3);
    private static final long SHORT_SESSION = TimeUnit.HOURS.toMillis(12);

    /** Permissions copied into a web session when it is created. */
    static final List<String> BASE_PERMISSIONS = List.of(
            "banova.ban", "banova.tempban", "banova.ipban", "banova.mute", "banova.tempmute", "banova.ipmute",
            "banova.warn", "banova.kick", "banova.unban", "banova.unmute", "banova.unwarn", "banova.punish",
            "banova.history", "banova.check", "banova.list", "banova.notes", "banova.notes.add", "banova.notes.delete",
            "banova.evidence", "banova.evidence.delete", "banova.appeals", "banova.alts", "banova.ips",
            "banova.staffhistory", "banova.staffrollback", "banova.edit", "banova.audit", "banova.lockdown",
            "banova.config.edit", "banova.web", "banova.web.admin", "banova.exempt.bypass", "banova.limit.bypass");

    private final BanovaCore core;
    private final RateLimiter limiter = new RateLimiter();
    private final Map<String, byte[]> staticCache = new ConcurrentHashMap<>();
    private volatile HttpServer server;
    private ExecutorService pool;
    private ApiRoutes routes;
    private String boundTo = "";

    public WebServer(BanovaCore core) {
        this.core = core;
    }

    // ------------------------------------------------------------------ lifecycle

    public synchronized void start() {
        if (server != null) {
            return;
        }
        if (!core.config().getBoolean("web.enabled", false)) {
            return;
        }
        String bind = core.config().getString("web.bind", "0.0.0.0");
        int port = core.config().getInt("web.port", 8090);
        try {
            HttpServer http = HttpServer.create(new InetSocketAddress(bind, port), 64);
            pool = Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "Banova-Web");
                t.setDaemon(true);
                return t;
            });
            routes = new ApiRoutes(core, this);
            http.setExecutor(pool);
            http.createContext("/", this::handle);
            http.start();
            server = http;
            boundTo = bind + ":" + port;
            core.platform().logger().info("Web panel listening on " + boundTo + " - open " + publicUrl());
        } catch (IOException | RuntimeException e) {
            core.platform().logger().warn("Could not start the web panel on " + bind + ":" + port + ": " + e.getMessage()
                    + " (is the port already in use or blocked by your host?)");
            if (pool != null) {
                pool.shutdownNow();
            }
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        staticCache.clear();
    }

    /** Applies config changes after /banova reload. */
    public synchronized void restart() {
        String wanted = core.config().getString("web.bind", "0.0.0.0") + ":" + core.config().getInt("web.port", 8090);
        boolean enabled = core.config().getBoolean("web.enabled", false);
        if (server != null && (!enabled || !wanted.equals(boundTo))) {
            stop();
        }
        staticCache.clear();
        if (enabled) {
            start();
        }
    }

    public boolean running() {
        return server != null;
    }

    public String publicUrl() {
        String url = core.config().getString("web.public-url", "").trim();
        if (url.isEmpty()) {
            url = "http://localhost:" + core.config().getInt("web.port", 8090);
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    boolean secureCookies() {
        return publicUrl().startsWith("https://");
    }

    RateLimiter limiter() {
        return limiter;
    }

    // ------------------------------------------------------------------ request dispatch

    private void handle(HttpExchange raw) {
        WebExchange ex = new WebExchange(raw, core.config().getBoolean("web.trust-proxy", false));
        try {
            String path = ex.path();
            if (path.startsWith("/api/")) {
                routes.handle(ex);
                return;
            }
            if (!ex.method().equals("GET") && !ex.method().equals("HEAD")) {
                ex.error(405, "method", null);
                return;
            }
            switch (path) {
                case "/", "/index.html" -> serveStatic(ex, "index.html");
                case "/appeal", "/appeal/" -> serveStatic(ex, "appeal.html");
                default -> {
                    if (path.startsWith("/assets/") && path.matches("/assets/[a-z0-9._-]+")) {
                        serveStatic(ex, path.substring("/assets/".length()));
                    } else {
                        ex.error(404, "not_found", null);
                    }
                }
            }
        } catch (Throwable t) {
            core.platform().logger().error("Web request failed: " + raw.getRequestURI(), t);
            try {
                ex.error(500, "internal", null);
            } catch (IOException ignored) {
                raw.close();
            }
        }
    }

    private void serveStatic(WebExchange ex, String file) throws IOException {
        byte[] data = staticCache.computeIfAbsent(file, f -> {
            try (InputStream in = WebServer.class.getResourceAsStream("/banova/web/" + f)) {
                return in == null ? new byte[0] : in.readAllBytes();
            } catch (IOException e) {
                return new byte[0];
            }
        });
        if (data.length == 0) {
            ex.error(404, "not_found", null);
            return;
        }
        String type;
        if (file.endsWith(".html")) {
            type = "text/html; charset=utf-8";
            ex.responseHeaders().set("Content-Security-Policy",
                    "default-src 'self'; img-src 'self' data: https://mc-heads.net; style-src 'self'; script-src 'self';"
                            + " connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
        } else if (file.endsWith(".js")) {
            type = "text/javascript; charset=utf-8";
        } else if (file.endsWith(".css")) {
            type = "text/css; charset=utf-8";
        } else if (file.endsWith(".svg")) {
            type = "image/svg+xml";
        } else if (file.endsWith(".png")) {
            type = "image/png";
        } else {
            type = "application/octet-stream";
        }
        ex.responseHeaders().set("Cache-Control", file.endsWith(".html") ? "no-cache" : "public, max-age=300");
        ex.send(200, type, data);
    }

    // ------------------------------------------------------------------ permissions snapshot

    /** Every permission that matters inside the panel, including limit groups and template permissions. */
    List<String> relevantPermissions() {
        Set<String> perms = new LinkedHashSet<>(BASE_PERMISSIONS);
        YamlConfig limits = core.config().section("punishments.limits");
        for (String group : limits.keys("")) {
            perms.add("banova.limit." + group.toLowerCase(java.util.Locale.ROOT));
        }
        for (TemplateService.Template t : core.templates().all()) {
            if (t.permission() != null && !t.permission().isBlank()) {
                perms.add(t.permission());
            }
        }
        return new ArrayList<>(perms);
    }

    Set<String> snapshot(CommandActor actor) {
        Set<String> out = new LinkedHashSet<>();
        if (actor.isConsole()) {
            out.add("*");
            return out;
        }
        for (String p : relevantPermissions()) {
            if (actor.hasPermission(p)) {
                out.add(p);
            }
        }
        return out;
    }

    private static String pack(Set<String> perms) {
        List<String> shortNames = new ArrayList<>();
        for (String p : perms) {
            shortNames.add(p.startsWith("banova.") ? p.substring(7) : "!" + p);
        }
        return String.join(",", shortNames);
    }

    private static Set<String> unpack(String packed) {
        Set<String> out = new LinkedHashSet<>();
        for (String s : packed.split(",")) {
            if (s.isEmpty()) {
                continue;
            }
            if (s.equals("*")) {
                out.add("*");
            } else {
                out.add(s.startsWith("!") ? s.substring(1) : "banova." + s);
            }
        }
        return out;
    }

    /** Called when a player joins: refreshes or removes their panel access. */
    public void refreshPermissions(CommandActor player) {
        if (player.uuid() == null) {
            return;
        }
        try {
            if (core.records().sessions(player.uuid()).isEmpty()) {
                return;
            }
            if (!player.hasPermission("banova.web")) {
                core.records().deleteSessionsOf(player.uuid());
                return;
            }
            core.records().updateSessionPermissions(player.uuid(), snapshot(player));
        } catch (RuntimeException e) {
            core.platform().logger().warn("Could not refresh web permissions: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ login flow

    record PendingLogin(String code, String poll) {
    }

    /** Browser side: creates a code to be approved in game/console. Stored in the DB for network-wide approval. */
    PendingLogin startLogin(String ip, String userAgent, boolean remember) {
        String code = Codes.humanCode();
        String poll = Codes.token(32);
        String value = String.join("|", Codes.sha256(poll), String.valueOf(System.currentTimeMillis()), remember ? "1" : "0",
                ip == null ? "" : ip.replace("|", ""), Text.truncate(userAgent == null ? "" : userAgent.replace("|", ""), 150));
        core.records().setMeta("weblogin:" + code, value);
        return new PendingLogin(code, poll);
    }

    /**
     * Staff side: approves a code typed in game/console. Creates the session whose token is the
     * browser's secret poll token, so only that browser can use it.
     */
    public boolean approveCode(String rawCode, CommandActor actor) {
        String code = Codes.normalizeHumanCode(rawCode);
        String value = core.records().meta("weblogin:" + code);
        if (value == null) {
            return false;
        }
        core.records().setMeta("weblogin:" + code, null);
        String[] parts = value.split("\\|", -1);
        if (parts.length < 5) {
            return false;
        }
        long created;
        try {
            created = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (System.currentTimeMillis() - created > CODE_TTL) {
            return false;
        }
        boolean remember = parts[2].equals("1");
        Set<String> perms = snapshot(actor);
        if (!perms.contains("*") && !perms.contains("banova.web")) {
            return false;
        }
        createSession(parts[0], actor.uuid(), actor.name(), perms, remember, parts[3], parts[4]);
        core.audit().log(actor.ref(), "WEB_LOGIN", actor.uuid(), actor.name(), 0, "code · " + parts[3] + " · " + Text.truncate(parts[4], 60));
        actor.send(core.messages().get("web.code-approved-details", "ip", parts[3].isEmpty() ? "?" : parts[3],
                "browser", browserName(parts[4])));
        return true;
    }

    /** Clickable one-time login link for players (/banova web). */
    public String createLoginLink(CommandActor actor) {
        if (!running() || actor.isConsole()) {
            return null;
        }
        Set<String> perms = snapshot(actor);
        String token = Codes.token(24);
        String value = String.join("|", String.valueOf(System.currentTimeMillis()), actor.uuid() == null ? "" : actor.uuid().toString(),
                actor.name(), pack(perms));
        core.records().setMeta("weblink:" + Codes.sha256(token).substring(0, 40), value);
        return publicUrl() + "/#/link/" + token;
    }

    /** Consumes a one-time link token and returns the new session token, or null. */
    String consumeLink(String token, boolean remember, String ip, String userAgent) {
        if (token == null || token.length() > 100) {
            return null;
        }
        String key = "weblink:" + Codes.sha256(token).substring(0, 40);
        String value = core.records().meta(key);
        if (value == null) {
            return null;
        }
        core.records().setMeta(key, null);
        String[] parts = value.split("\\|", -1);
        if (parts.length < 4) {
            return null;
        }
        try {
            if (System.currentTimeMillis() - Long.parseLong(parts[0]) > LINK_TTL) {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        UUID uuid = parts[1].isEmpty() ? null : UUID.fromString(parts[1]);
        String sessionToken = Codes.token(32);
        createSession(Codes.sha256(sessionToken), uuid, parts[2], unpack(parts[3]), remember, ip, userAgent);
        core.audit().log(new dev.banova.core.model.ActorRef(uuid, parts[2], dev.banova.core.model.Source.WEB), "WEB_LOGIN",
                uuid, parts[2], 0, "link · " + ip);
        return sessionToken;
    }

    private void createSession(String tokenHash, UUID uuid, String name, Set<String> perms, boolean remember, String ip, String ua) {
        long now = System.currentTimeMillis();
        long ttl = remember ? sessionLength() : SHORT_SESSION;
        WebSession session = new WebSession(Codes.token(6), tokenHash, uuid, name, perms, now, now, now + ttl, remember, ip, ua);
        core.records().addSession(session);
    }

    long sessionLength() {
        long days = Math.max(1, core.config().getInt("web.session-days", 30));
        return TimeUnit.DAYS.toMillis(days);
    }

    /** Finds the session of a request; extends "remember me" sessions while they are used. */
    WebSession authenticate(WebExchange ex) {
        String token = ex.cookie(COOKIE);
        if (token == null || token.isEmpty() || token.length() > 100) {
            return null;
        }
        return sessionForToken(token);
    }

    WebSession sessionForToken(String token) {
        WebSession s = core.records().sessionByHash(Codes.sha256(token));
        if (s == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (s.expiresAt() <= now) {
            core.records().deleteSession(s.id());
            return null;
        }
        if (now - s.lastUsed() > 60_000) {
            long expires = s.remember() ? now + sessionLength() : s.expiresAt();
            core.records().touchSession(s.id(), now, expires);
        }
        return s;
    }

    String cookieFor(String token, boolean remember) {
        StringBuilder sb = new StringBuilder(COOKIE).append('=').append(token).append("; Path=/; HttpOnly; SameSite=Strict");
        if (remember) {
            sb.append("; Max-Age=").append(sessionLength() / 1000);
        }
        if (secureCookies()) {
            sb.append("; Secure");
        }
        return sb.toString();
    }

    static String clearCookie() {
        return COOKIE + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0";
    }

    static String browserName(String ua) {
        if (ua == null || ua.isBlank()) {
            return "?";
        }
        String os = ua.contains("Windows") ? "Windows" : ua.contains("Android") ? "Android" : ua.contains("iPhone") || ua.contains("iPad") ? "iOS"
                : ua.contains("Mac OS") ? "macOS" : ua.contains("Linux") ? "Linux" : "";
        String browser = ua.contains("Edg/") ? "Edge" : ua.contains("OPR/") ? "Opera" : ua.contains("Firefox/") ? "Firefox"
                : ua.contains("Chrome/") ? "Chrome" : ua.contains("Safari/") ? "Safari" : "Browser";
        return os.isEmpty() ? browser : browser + " · " + os;
    }

    static long parseDuration(String s) {
        return Durations.parse(s);
    }
}
