package dev.banova.core;

import dev.banova.api.BanovaApi;
import dev.banova.api.BanovaListener;
import dev.banova.core.command.CommandManager;
import dev.banova.core.config.Messages;
import dev.banova.core.config.YamlConfig;
import dev.banova.core.filter.ChatFilter;
import dev.banova.core.importer.ImportManager;
import dev.banova.core.platform.Platform;
import dev.banova.core.service.AppealService;
import dev.banova.core.service.AuditService;
import dev.banova.core.service.DiscordWebhook;
import dev.banova.core.service.Formatter;
import dev.banova.core.service.LockdownService;
import dev.banova.core.service.MuteCache;
import dev.banova.core.service.PlayerService;
import dev.banova.core.service.PunishmentService;
import dev.banova.core.service.SyncService;
import dev.banova.core.service.TemplateService;
import dev.banova.core.storage.Database;
import dev.banova.core.storage.PlayerRepository;
import dev.banova.core.storage.PunishmentRepository;
import dev.banova.core.storage.RecordRepository;
import dev.banova.core.web.WebServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wires every Banova service together. One instance per running plugin.
 */
public final class BanovaCore {

    private final Platform platform;
    private final List<BanovaListener> apiListeners = new CopyOnWriteArrayList<>();

    private ExecutorService executor;
    private ScheduledExecutorService scheduler;

    private volatile YamlConfig config;
    private volatile Messages messages;
    private Database database;
    private PunishmentRepository punishmentRepo;
    private PlayerRepository playerRepo;
    private RecordRepository records;

    private Formatter formatter;
    private PunishmentService punishments;
    private PlayerService players;
    private TemplateService templates;
    private AppealService appeals;
    private AuditService audit;
    private LockdownService lockdown;
    private SyncService sync;
    private MuteCache mutes;
    private DiscordWebhook discord;
    private ChatFilter filter;
    private CommandManager commands;
    private ImportManager importer;
    private WebServer web;
    private String serverName;

    public BanovaCore(Platform platform) {
        this.platform = platform;
    }

    // ------------------------------------------------------------------ lifecycle

    public void enable() throws Exception {
        executor = Executors.newFixedThreadPool(6, threads("Banova-Worker"));
        scheduler = Executors.newSingleThreadScheduledExecutor(threads("Banova-Scheduler"));

        Files.createDirectories(platform.dataFolder());
        saveDefaults();
        loadFiles();

        database = new Database(config.section("storage"), platform.dataFolder(), platform.logger());
        punishmentRepo = new PunishmentRepository(database);
        playerRepo = new PlayerRepository(database);
        records = new RecordRepository(database);

        formatter = new Formatter(this);
        mutes = new MuteCache();
        audit = new AuditService(this);
        templates = new TemplateService(this);
        punishments = new PunishmentService(this);
        players = new PlayerService(this);
        appeals = new AppealService(this);
        lockdown = new LockdownService(this);
        sync = new SyncService(this);
        discord = new DiscordWebhook(this);
        filter = new ChatFilter(this);
        importer = new ImportManager(this);
        commands = new CommandManager(this);
        web = new WebServer(this);

        loadRuntimeConfig();
        lockdown.load();
        sync.start();

        scheduler.scheduleWithFixedDelay(() -> safely("expiry", () -> punishmentRepo.deactivateExpired()), 30, 60, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(() -> safely("sessions", () -> records.deleteExpiredSessions()), 1, 30, TimeUnit.MINUTES);

        if (config.getBoolean("web.enabled", false)) {
            web.start();
        }
        BanovaApi.Holder.set(new ApiImpl(this));
        platform.logger().info("Banova " + platform.version() + " enabled on " + platform.platformName()
                + " (storage: " + database.kind() + (sync.enabled() ? ", network sync on" : "") + ")");
    }

    public synchronized void refreshRulesAndSettings() throws IOException {
        loadFiles();
        loadRuntimeConfig();
    }

    public synchronized void reload() throws IOException {
        saveDefaults();
        loadFiles();
        loadRuntimeConfig();
        lockdown.load();
        web.restart();
    }

    public void disable() {
        BanovaApi.Holder.set(null);
        if (web != null) {
            web.stop();
        }
        if (sync != null) {
            sync.stop();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (database != null) {
            database.close();
        }
    }

    private void loadRuntimeConfig() throws IOException {
        serverName = config.getString("server-name", "server").trim();
        if (serverName.isEmpty()) {
            serverName = "server";
        }
        formatter.reload();
        templates.load(loadFile("templates.yml"));
        filter.load(loadFile("filter.yml"));
    }

    private void loadFiles() throws IOException {
        config = loadFile("config.yml");
        serverName = config.getString("server-name", "server");
        String lang = config.getString("language", "en").toLowerCase(Locale.ROOT).trim();
        YamlConfig english = YamlConfig.loadResource("/banova/lang/en.yml");
        YamlConfig bundled = lang.equals("en") ? english : withDefaults("/banova/lang/" + lang + ".yml", english);
        Path userLang = platform.dataFolder().resolve("lang").resolve(lang + ".yml");
        YamlConfig langConfig = Files.exists(userLang) ? YamlConfig.load(userLang, bundled) : bundled;
        messages = new Messages(langConfig, lang);
    }

    private static YamlConfig withDefaults(String resource, YamlConfig defaults) {
        try (InputStream in = BanovaCore.class.getResourceAsStream(resource)) {
            if (in == null) {
                return defaults;
            }
            return YamlConfig.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8), defaults);
        } catch (IOException e) {
            return defaults;
        }
    }

    /** Loads a user file with the bundled copy as defaults for missing keys. */
    private YamlConfig loadFile(String name) throws IOException {
        YamlConfig defaults = YamlConfig.loadResource("/banova/" + name);
        Path file = platform.dataFolder().resolve(name);
        if (!Files.exists(file)) {
            return defaults;
        }
        try {
            return YamlConfig.load(file, defaults);
        } catch (RuntimeException e) {
            platform.logger().warn(name + " has a YAML error, using defaults until it is fixed: " + e.getMessage());
            return defaults;
        }
    }

    private void saveDefaults() throws IOException {
        Path folder = platform.dataFolder();
        copyIfMissing("config.yml", folder.resolve("config.yml"));
        // Templates contain reasons shown to players, so start with the server's language.
        String configText = Files.readString(folder.resolve("config.yml"), StandardCharsets.UTF_8);
        boolean spanish = configText.matches("(?s).*(?m)^language:\\s*\"?es\"?\\s*$.*");
        copyIfMissing(spanish ? "templates-es.yml" : "templates.yml", folder.resolve("templates.yml"));
        copyIfMissing("filter.yml", folder.resolve("filter.yml"));
        copyIfMissing("lang/en.yml", folder.resolve("lang").resolve("en.yml"));
        copyIfMissing("lang/es.yml", folder.resolve("lang").resolve("es.yml"));
    }

    private void copyIfMissing(String resource, Path target) throws IOException {
        if (Files.exists(target)) {
            return;
        }
        Files.createDirectories(target.getParent());
        try (InputStream in = BanovaCore.class.getResourceAsStream("/banova/" + resource)) {
            if (in != null) {
                Files.copy(in, target);
            }
        }
    }

    // ------------------------------------------------------------------ threading

    private static ThreadFactory threads(String name) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /** Runs a task on Banova's worker threads. */
    public void async(Runnable task) {
        if (executor == null || executor.isShutdown()) {
            return;
        }
        executor.execute(() -> safely("task", task));
    }

    private void safely(String what, Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            platform.logger().error("Banova " + what + " failed", t);
        }
    }

    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    public ExecutorService executor() {
        return executor;
    }

    // ------------------------------------------------------------------ accessors

    public Platform platform() {
        return platform;
    }

    public YamlConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    public Database database() {
        return database;
    }

    public PunishmentRepository punishmentRepo() {
        return punishmentRepo;
    }

    public PlayerRepository playerRepo() {
        return playerRepo;
    }

    public RecordRepository records() {
        return records;
    }

    public Formatter formatter() {
        return formatter;
    }

    public PunishmentService punishments() {
        return punishments;
    }

    public PlayerService players() {
        return players;
    }

    public TemplateService templates() {
        return templates;
    }

    public AppealService appeals() {
        return appeals;
    }

    public AuditService audit() {
        return audit;
    }

    public LockdownService lockdown() {
        return lockdown;
    }

    public SyncService sync() {
        return sync;
    }

    public MuteCache mutes() {
        return mutes;
    }

    public DiscordWebhook discord() {
        return discord;
    }

    public ChatFilter filter() {
        return filter;
    }

    public CommandManager commands() {
        return commands;
    }

    public ImportManager importer() {
        return importer;
    }

    public WebServer web() {
        return web;
    }

    public String serverName() {
        return serverName;
    }

    public List<BanovaListener> apiListeners() {
        return apiListeners;
    }
}
