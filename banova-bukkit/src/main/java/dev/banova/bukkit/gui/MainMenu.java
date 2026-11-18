package dev.banova.bukkit.gui;

import dev.banova.api.PunishmentType;
import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Records.AppealStatus;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.storage.PunishmentRepository;
import org.bukkit.entity.Player;

/**
 * /banova and /punish without arguments: entry point to every tool.
 */
public final class MainMenu extends Menu {

    private long bans;
    private long mutes;
    private long warns;
    private long appeals;

    public MainMenu(BanovaBukkit plugin, Player viewer, Menu parent) {
        super(plugin, viewer, parent);
    }

    @Override
    protected String title() {
        return msg("gui.main.title");
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void load() {
        bans = count(PunishmentType.BAN);
        mutes = count(PunishmentType.MUTE);
        warns = count(PunishmentType.WARN);
        appeals = core.records().countAppeals(AppealStatus.PENDING);
    }

    private long count(PunishmentType type) {
        PunishmentRepository.Filter f = new PunishmentRepository.Filter();
        f.type = type;
        f.activeOnly = true;
        return core.punishmentRepo().count(f);
    }

    @Override
    protected void render() {
        set(9, Items.item("PLAYER_HEAD", msg("gui.review.people"), null));
        if (actor.hasPermission("banova.list") || actor.hasPermission("banova.history"))
            set(18, Items.item("BOOKSHELF", msg("gui.review.cases"), null));
        if (actor.hasPermission("banova.appeals") || actor.hasPermission("banova.audit") || actor.hasPermission("banova.lockdown"))
            set(27, Items.item("LECTERN", msg("gui.review.staff"), null));
        set(11, Items.item("SPYGLASS", msg("gui.main.players"), core.messages().lines("gui.main.players-lore",
                "count", core.platform().onlinePlayers().size())), type -> new OnlinePlayersMenu(plugin, viewer, this).open());
        set(12, Items.item("NAME_TAG", msg("gui.main.search"), core.messages().lines("gui.main.search-lore")),
                type -> prompt("gui.prompt.search", input -> {
                    PlayerRecord record = core.players().resolve(input);
                    if (record == null || record.uuid() == null) {
                        actor.send(msg("error.player-not-found", "player", input));
                        open();
                        return;
                    }
                    new PlayerMenu(plugin, viewer, this, record).open();
                }));
        if (actor.hasPermission("banova.list")) {
            set(20, Items.item("BARRIER", msg("gui.main.bans"), core.messages().lines("gui.main.count-lore", "count", bans)),
                    type -> new ListMenu(plugin, viewer, this, PunishmentType.BAN).open());
            set(21, Items.item("WRITABLE_BOOK", msg("gui.main.mutes"), core.messages().lines("gui.main.count-lore", "count", mutes)),
                    type -> new ListMenu(plugin, viewer, this, PunishmentType.MUTE).open());
            set(22, Items.item("BELL", msg("gui.main.warns"), core.messages().lines("gui.main.count-lore", "count", warns)),
                    type -> new ListMenu(plugin, viewer, this, PunishmentType.WARN).open());
        }
        if (actor.hasPermission("banova.history")) {
            set(23, Items.item("BOOK", msg("gui.main.recent"), core.messages().lines("gui.main.recent-lore")),
                    type -> new ListMenu(plugin, viewer, this, null).open());
        }
        if (actor.hasPermission("banova.appeals")) {
            set(29, Items.item(appeals > 0 ? "WRITABLE_BOOK" : "BOOK", msg("gui.main.appeals"),
                    core.messages().lines("gui.main.appeals-lore", "count", appeals)), type -> new AppealsMenu(plugin, viewer, this).open());
        }
        if (actor.hasPermission("banova.audit")) {
            set(30, Items.item("CLOCK", msg("gui.main.audit"), core.messages().lines("gui.main.audit-lore")),
                    type -> new AuditMenu(plugin, viewer, this).open());
        }
        if (actor.hasPermission("banova.lockdown")) {
            boolean on = core.lockdown().active();
            set(31, Items.item(on ? "IRON_DOOR" : "OAK_DOOR", msg(on ? "gui.main.lockdown-on" : "gui.main.lockdown-off"),
                    core.messages().lines(on ? "gui.main.lockdown-on-lore" : "gui.main.lockdown-off-lore",
                            "reason", on ? core.lockdown().reason() : "")), type -> {
                if (on) {
                    review(msg("gui.review.open-server"), core.serverName(), () -> {
                        core.commands().get("lockdown").execute(actor, "lockdown", new String[]{"off"});
                        open();
                    });
                } else {
                    prompt("gui.prompt.lockdown-reason", input -> {
                        String reason = input.equals("-") ? "" : input;
                        review(msg("gui.review.close-server"), core.serverName() + " · " + reason, () -> {
                            core.commands().get("lockdown").execute(actor, "lockdown", ("on " + reason).trim().split(" "));
                            open();
                        });
                    });
                }
            });
        }
        if (actor.hasPermission("banova.web")) {
            set(42, Items.item("COMPASS", msg("gui.main.web"), core.messages().lines(core.web().running()
                    ? "gui.main.web-lore" : "gui.main.web-off-lore")), type -> {
                viewer.closeInventory();
                core.async(() -> core.commands().get("banova").execute(actor, "banova", new String[]{"web"}));
            });
        }
        set(0, Items.item("KNOWLEDGE_BOOK", msg("gui.main.info", "version", core.platform().version()),
                core.messages().lines("gui.main.info-lore", "storage", core.database().kind(),
                        "server", core.serverName())), type -> {
            viewer.closeInventory();
            core.async(() -> core.commands().get("banova").execute(actor, "banova", new String[]{"help"}));
        });
        set(40, Items.item("BARRIER", msg("gui.close"), null), type -> viewer.closeInventory());
        fillEmpty();
    }

    static void openPlayer(BanovaBukkit plugin, Player viewer, Menu parent, CommandActor actor, String name) {
        plugin.core().async(() -> {
            PlayerRecord record = plugin.core().players().resolve(name);
            if (record == null || record.uuid() == null) {
                actor.send(plugin.core().messages().get("error.player-not-found", "player", name));
                return;
            }
            new PlayerMenu(plugin, viewer, parent, record).open();
        });
    }
}
