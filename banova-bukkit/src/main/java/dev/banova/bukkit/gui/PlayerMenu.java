package dev.banova.bukkit.gui;

import dev.banova.api.PunishmentType;
import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.service.TemplateService;
import dev.banova.core.service.TemplateService.Category;
import dev.banova.core.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Everything about one player: status, escalation templates, custom punishments, history, notes, alts.
 */
public final class PlayerMenu extends Menu {

    private static final int ROUTES_PER_ROW = 6;
    private int page;
    private int pageCount = 1;
    private final EnumMap<Category, List<TemplateService.NextStep>> visible = new EnumMap<>(Category.class);
    private PlayerRecord target;
    private Punishment ban;
    private Punishment mute;
    private int warns;
    private long total;
    private long notes = -1;
    private int alts = -1;

    public PlayerMenu(BanovaBukkit plugin, Player viewer, Menu parent, PlayerRecord target) {
        super(plugin, viewer, parent);
        this.target = target;
    }

    @Override
    protected String title() {
        return msg("gui.dossier.title", "player", target.name());
    }

    @Override
    protected void load() {
        PlayerRecord fresh = core.players().byUuid(target.uuid());
        if (fresh != null) {
            target = fresh;
        }
        ban = core.punishments().activeBan(target.uuid(), target.lastIp());
        mute = core.punishments().activeMute(target.uuid(), target.lastIp());
        warns = core.punishments().activeWarns(target.uuid()).size();
        total = core.punishmentRepo().countHistory(target.uuid(), null);
        if (actor.hasPermission("banova.notes")) {
            notes = core.records().countNotes(target.uuid());
        }
        if (actor.hasPermission("banova.alts")) {
            alts = core.playerRepo().alts(target.uuid()).size();
        }
        visible.clear();
        if (actor.hasPermission("banova.punish")) {
            var groups = new EnumMap<Category, List<TemplateService.Template>>(Category.class);
            pageCount = 1;
            for (Category group : Category.values()) {
                var routes = core.templates().all().stream().filter(t -> t.category() == group).toList();
                groups.put(group, routes);
                pageCount = Math.max(pageCount, (routes.size() + ROUTES_PER_ROW - 1) / ROUTES_PER_ROW);
            }
            page = Math.min(page, pageCount - 1);
            groups.replaceAll((group, routes) -> {
                int start = Math.min(page * ROUTES_PER_ROW, routes.size());
                return routes.subList(start, Math.min(start + ROUTES_PER_ROW, routes.size()));
            });
            var previews = core.templates().previews(groups.values().stream().flatMap(List::stream).toList(), target.uuid());
            groups.forEach((group, routes) -> visible.put(group, routes.stream().map(t -> previews.get(t.id())).toList()));
        }
    }

    @Override
    protected void render() {
        boolean online = core.platform().player(target.uuid()) != null;
        List<String> head = new ArrayList<>(core.messages().lines("gui.player.head-lore",
                "ban", ban == null ? msg("gui.player.status-none") : msg("gui.player.status-banned", "remaining", core.formatter().remaining(ban)),
                "mute", mute == null ? msg("gui.player.status-none") : msg("gui.player.status-muted", "remaining", core.formatter().remaining(mute)),
                "warns", warns, "total", total, "notes", notes < 0 ? "-" : notes, "alts", alts < 0 ? "-" : alts,
                "first", core.formatter().date(target.firstSeen()),
                "last", online ? msg("check.online-now") : core.formatter().date(target.lastSeen()),
                "ip", target.lastIp() == null ? "-" : actor.hasPermission("banova.ips") ? target.lastIp() : Text.maskIp(target.lastIp())));
        set(0, Items.head(target.uuid(), msg("gui.player.head", "player", target.name(), "online", online ? "&a●" : "&7●"), head));

        if (ban != null && actor.hasPermission("banova.unban")) {
            set(2, Items.item("LIME_DYE", msg("gui.player.unban"), core.messages().lines("gui.player.revoke-lore", "reason", ban.reason)),
                    type -> revoke(ban));
        }
        if (mute != null && actor.hasPermission("banova.unmute")) {
            set(6, Items.item("LIME_DYE", msg("gui.player.unmute"), core.messages().lines("gui.player.revoke-lore", "reason", mute.reason)),
                    type -> revoke(mute));
        }

        if (!visible.isEmpty() && actor.hasPermission("banova.punish")) {
            var rows = java.util.Arrays.stream(Category.values()).filter(c -> !visible.get(c).isEmpty()).toList();
            for (int i = 0; i < rows.size(); i++) {
                int row = rows.size() == 1 ? 2 : rows.size() == 2 ? 1 + 2 * i : 1 + i;
                routeRow(rows.get(i), row);
            }
        }
        if (actor.hasPermission("banova.inspect")) {
            quick(9, "ENDER_EYE", "watch", online);
            quick(18, "ENDER_PEARL", "visit", online);
            quick(27, "CHEST", "inventory", online);
        }
        if (actor.hasPermission("banova.history"))
            set(45, Items.item("BOOK", msg("gui.player.history"), core.messages().lines("gui.main.count-lore", "count", total)),
                    click -> new HistoryMenu(plugin, viewer, this, target).open());
        if (actor.hasPermission("banova.evidence") && core.config().getBoolean("chat.recent-register", true))
            set(46, Items.item("WRITABLE_BOOK", msg("gui.route.chat-log"), List.of(msg("gui.route.chat-hint"))), click -> {
                viewer.closeInventory();
                actor.send(msg("gui.route.chat-title", "player", target.name()));
                var log = plugin.recentChat().forPlayer(target.uuid());
                if (log.isEmpty()) actor.send(msg("gui.empty"));
                for (var line : log) actor.send("&8" + core.formatter().date(line.time()) + " &7· &f" + Text.strip(line.text()));
            });
        if (actor.hasPermission("banova.notes"))
            set(47, Items.item("OAK_HANGING_SIGN", msg("gui.player.notes"), core.messages().lines("gui.main.count-lore", "count", notes)),
                    click -> new NotesMenu(plugin, viewer, this, target).open());
        if (actor.hasPermission("banova.appeals"))
            set(pageCount > 1 ? 37 : 48, Items.item("GOAT_HORN", msg("gui.appeals.title"), List.of(target.name())),
                    click -> new AppealsMenu(plugin, viewer, this, target.uuid()).open());
        if (actor.hasPermission("banova.alts"))
            set(pageCount > 1 ? 44 : 50, Items.item("ARMOR_STAND", msg("gui.player.alts"), core.messages().lines("gui.main.count-lore", "count", alts)),
                    click -> new AltsMenu(plugin, viewer, this, target).open());
        if (page > 0) set(48, Items.item("ARROW", msg("gui.previous-page", "page", page), null), c -> { page--; open(); });
        if (page + 1 < pageCount) set(50, Items.item("ARROW", msg("gui.next-page", "page", page + 2), null), c -> { page++; open(); });
        if (actor.hasPermission("banova.ban") || actor.hasPermission("banova.tempban") || actor.hasPermission("banova.mute")
                || actor.hasPermission("banova.tempmute") || actor.hasPermission("banova.kick") || actor.hasPermission("banova.warn"))
            set(51, Items.item("COMMAND_BLOCK", msg("gui.route.manual-sanction"), List.of(target.name())),
                    click -> new ManualTypeMenu(plugin, viewer, this, target).open());
        set(49, Items.item("BARRIER", msg("gui.close"), null), click -> viewer.closeInventory());
        if (parent != null) set(52, Items.item("ARROW", msg("gui.back"), List.of(parent.title())), click -> parent.open());
        set(53, Items.item("SUNFLOWER", msg("gui.route.refresh"), null), click -> open());
        var frame = Items.item("LIGHT_BLUE_STAINED_GLASS_PANE", " ", null);
        for (int slot : new int[]{1,2,3,4,5,6,7,8,10,17,19,26,28,35,37,38,39,40,41,42,43,44}) {
            if (getInventory().getItem(slot) == null) set(slot, frame);
        }
        var bar = Items.item("BLACK_STAINED_GLASS_PANE", " ", null);
        for (int slot = 45; slot < 54; slot++) if (getInventory().getItem(slot) == null) set(slot, bar);
    }

    private void routeRow(Category group, int row) {
        var routes = visible.get(group);
        int slot = row * 9 + 2 + (6 - routes.size()) / 2;
        for (var next : routes) {
            var route = next.template();
            boolean allowed = core.templates().canUse(actor, route);
            List<String> lore = new ArrayList<>(List.of(msg("gui.dossier." + group.key()), "&7" + route.reason(),
                    msg("gui.route.priors", "count", next.offenses()), ""));
            lore.addAll(RouteMenu.ladder(this, next, next.index()));
            lore.add("");
            lore.add(msg(allowed ? "gui.route.left" : "gui.route.locked"));
            if (allowed) lore.add(msg("gui.route.right"));
            ItemStack item = Items.item(allowed ? route.icon() : "GRAY_DYE", "&f" + route.name(), lore);
            set(slot++, item, allowed ? click -> {
                if (click.isRightClick()) new RouteMenu(plugin, viewer, this, target, route).open();
                else if (click.isLeftClick()) new RouteReview(plugin, viewer, this, this, target, next, next.index()).open();
            } : null);
        }
    }

    private void quick(int slot, String material, String action, boolean online) {
        set(slot, Items.item(online ? material : "GRAY_DYE", msg("gui.route." + action),
                List.of(msg(online ? "gui.route." + action + "-hint" : "gui.route.offline"))), click -> {
            Player other = org.bukkit.Bukkit.getPlayer(target.uuid());
            if (other == null || !viewer.canSee(other) || !actor.hasPermission("banova.inspect")) {
                actor.send(msg("gui.route.offline")); return;
            }
            if (action.equals("watch") && (viewer.getGameMode() != org.bukkit.GameMode.SPECTATOR || viewer == other)) {
                actor.send(msg("gui.route.spectator-mode")); return;
            }
            plugin.runFor(other, () -> {
                if (action.equals("inventory")) {
                    var items = other.getInventory().getContents();
                    for (int i = 0; i < items.length; i++) if (items[i] != null) items[i] = items[i].clone();
                    plugin.runFor(viewer, () -> new InspectInventory(plugin, viewer, this, target.name(), items).open());
                    return;
                }
                var destination = other.getLocation();
                plugin.runFor(viewer, () -> {
                    viewer.closeInventory();
                    if (dev.banova.bukkit.BukkitText.PAPER) {
                        viewer.teleportAsync(destination).thenAccept(ok -> {
                            if (ok && action.equals("watch")) plugin.runFor(viewer, () -> {
                                if (other.isOnline() && viewer.getGameMode() == org.bukkit.GameMode.SPECTATOR) viewer.setSpectatorTarget(other);
                            });
                        });
                    } else if (viewer.teleport(destination) && action.equals("watch")) viewer.setSpectatorTarget(other);
                });
            });
        });
    }

    private void revoke(Punishment p) {
        prompt("gui.prompt.revoke-reason", input -> review(msg("gui.punishment.revoke"), p.targetName + " · #" + p.id + "\n" + input, () -> {
            if (core.punishments().revoke(p, actor.ref(), input.equals("-") ? null : input)) {
                actor.send(msg("revoke.success", core.formatter().placeholders(p)));
            }
            open();
        }));
    }

    PlayerRecord target() {
        return target;
    }
}
