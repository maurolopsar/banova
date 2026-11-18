package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.command.TemplateCommand;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.service.TemplateService.NextStep;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

final class RouteReview extends Menu {
    private final PlayerMenu dossier;
    private final PlayerRecord target;
    private final NextStep next;
    private final int selected;
    private String context = "";
    private boolean silent;
    private boolean submitted;

    RouteReview(BanovaBukkit plugin, Player viewer, Menu parent, PlayerMenu dossier, PlayerRecord target, NextStep next, int selected) {
        super(plugin, viewer, parent);
        this.dossier = dossier;
        this.target = target;
        this.next = next;
        this.selected = selected;
        silent = next.template().silent() || core.config().getBoolean("punishments.silent-by-default", false);
    }

    @Override protected String title() { return msg("gui.confirm.title"); }
    @Override protected int rows() { return 4; }

    @Override protected void render() {
        var route = next.template();
        List<String> lines = new ArrayList<>(List.of(msg("gui.route.target", "player", target.name()),
                msg("gui.route.level", "level", selected + 1, "step", RouteMenu.stepLabel(this, route, selected)),
                msg(selected == next.index() ? "gui.route.recommended" : "gui.route.manual"),
                msg("gui.route.context-value", "context", context.isBlank() ? "—" : context), ""));
        lines.addAll(RouteMenu.ladder(this, next, selected));
        set(13, Items.item(route.icon(), "&f" + route.name(), lines));
        boolean allowed = dev.banova.core.service.TemplateService.canApplyStep(actor, route.step(selected), selected != next.index());
        set(11, Items.item(allowed ? "LIME_STAINED_GLASS_PANE" : "GRAY_DYE", msg(allowed ? "gui.confirm.yes" : "gui.route.locked"),
                List.of(msg("gui.route.target", "player", target.name()))), allowed ? click -> {
            if (submitted) return;
            submitted = true;
            viewer.closeInventory();
            core.async(() -> {
                TemplateCommand.apply(core, actor, target, route, silent, selected == next.index() ? null : selected, context, selected);
                dossier.open();
            });
        } : null);
        if (actor.hasPermission("banova.evidence")) {
            set(15, Items.item("NAME_TAG", msg("gui.route.context"), List.of(msg("gui.route.context-hint"))), click ->
                    prompt("gui.route.prompt", input -> {
                        if (input.length() > 1000) actor.send(msg("gui.route.context-invalid"));
                        else context = input.equals("-") ? "" : input;
                        open();
                    }));
        }
        boolean forcedSilent = route.silent() || core.config().getBoolean("punishments.silent-by-default", false);
        set(33, Items.item(silent ? "ENDER_EYE" : "ENDER_PEARL", msg(silent ? "gui.silent-on" : "gui.silent-off"), null),
                forcedSilent ? null : click -> { silent = !silent; render(); });
        set(31, Items.item("BARRIER", msg("gui.confirm.no"), List.of(parent.title())), click -> parent.open());
        fillEmpty();
    }
}
