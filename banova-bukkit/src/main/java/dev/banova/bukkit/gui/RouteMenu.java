package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.service.TemplateService.NextStep;
import dev.banova.core.service.TemplateService.Template;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

final class RouteMenu extends Menu {
    private final PlayerMenu dossier;
    private final PlayerRecord target;
    private final Template route;
    private NextStep next;
    private List<Punishment> history;
    private int levelPage;
    private int historyPage;

    RouteMenu(BanovaBukkit plugin, Player viewer, PlayerMenu dossier, PlayerRecord target, Template route) {
        super(plugin, viewer, dossier);
        this.dossier = dossier;
        this.target = target;
        this.route = route;
    }

    @Override protected String title() { return msg("gui.route.title", "route", route.name()); }

    @Override protected void load() {
        next = core.templates().next(route, target.uuid());
        history = actor.hasPermission("banova.history")
                ? core.punishmentRepo().routeHistory(target.uuid(), route.id(), 8, historyPage * 7) : List.of();
    }

    @Override protected void render() {
        set(4, Items.item(route.icon(), route.name(), List.of(target.name(), msg("gui.route.pick"))));
        int first = levelPage * 14;
        for (int index = first; index < Math.min(first + 14, route.steps().size()); index++) {
            int selected = index;
            int offset = index - first;
            int columns = Math.min(7, route.steps().size() - first - offset / 7 * 7);
            int slot = 10 + offset / 7 * 9 + (7 - columns) / 2 + offset % 7;
            boolean recommended = selected == next.index();
            String material = recommended ? "ORANGE_STAINED_GLASS_PANE" :
                    selected < next.index() ? "GRAY_STAINED_GLASS_PANE" : "WHITE_STAINED_GLASS_PANE";
            boolean allowed = (recommended || actor.hasPermission("banova.route.level"))
                    && dev.banova.core.service.TemplateService.canApplyStep(actor, route.step(selected), !recommended);
            var item = Items.item(material, msg("gui.route.level", "level", selected + 1, "step", stepLabel(this, route, selected)),
                    List.of(msg(recommended ? "gui.route.recommended" : "gui.route.manual"), msg(allowed ? "gui.route.inspect" : "gui.route.locked")));
            item.setAmount(Math.min(64, selected + 1));
            set(slot, item, allowed ? click -> new RouteReview(plugin, viewer, this, dossier, target, next, selected).open() : null);
        }
        if (first > 0) set(9, Items.item("ARROW", msg("gui.previous-page", "page", levelPage), null), c -> { levelPage--; open(); });
        if (first + 14 < route.steps().size()) set(26, Items.item("ARROW", msg("gui.next-page", "page", levelPage + 2), null), c -> { levelPage++; open(); });
        if (actor.hasPermission("banova.history")) {
            set(28, Items.item("BOOK", msg("gui.route.prior"), List.of(msg("gui.route.revoke-hint"))));
            if (history.isEmpty()) set(40, Items.item("LIME_DYE", msg("gui.route.no-history"), null));
            int count = Math.min(7, history.size());
            for (int i = 0; i < count; i++) {
                Punishment p = history.get(i);
                set(37 + (7 - count) / 2 + i, Icons.punishment(core, p, "gui.click-details"),
                        click -> RevokeMenu.openEntry(plugin, viewer, this, p, click));
            }
            if (historyPage > 0) set(36, Items.item("ARROW", msg("gui.previous-page", "page", historyPage), null), c -> { historyPage--; open(); });
            if (history.size() > 7) set(44, Items.item("ARROW", msg("gui.next-page", "page", historyPage + 2), null), c -> { historyPage++; open(); });
        }
        backButton(49);
        fillEmpty();
    }

    static String stepLabel(Menu menu, Template route, int index) {
        var step = route.step(index);
        String duration = menu.core.formatter().stepDuration(step.type(), step.duration());
        return menu.core.formatter().typeName(step.type()) + (duration.isBlank() ? "" : " " + duration) + (step.ip() ? " · IP" : "");
    }

    static List<String> ladder(Menu menu, NextStep next, int selected) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < next.template().steps().size(); i++) {
            String color = i == selected ? "&6➥ " : i < next.index() ? "&8&m   " : "&7   ";
            lines.add(color + (i + 1) + " · " + dev.banova.core.util.Text.strip(stepLabel(menu, next.template(), i)));
        }
        return lines;
    }
}
