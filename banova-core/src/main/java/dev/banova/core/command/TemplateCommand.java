package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.service.PunishmentService;
import dev.banova.core.service.PunishmentService.Outcome;
import dev.banova.core.service.TemplateService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /punish &lt;player&gt; [template] — opens the punishment menu or applies the next escalation step.
 */
public final class TemplateCommand extends Command {

    public TemplateCommand(BanovaCore core) {
        super(core, "punish", "banova.punish", "p", "sanction", "sancionar");
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        boolean silentFlag = false;
        List<String> rest = new ArrayList<>();
        for (String a : args) {
            if (a.equalsIgnoreCase("-s")) {
                silentFlag = true;
            } else {
                rest.add(a);
            }
        }
        if (rest.isEmpty()) {
            if (actor.isPlayer() && core.platform().gui() != null) {
                core.platform().gui().openMain(actor);
            } else {
                usage(actor);
            }
            return;
        }
        PlayerRecord target = player(actor, rest.get(0));
        if (target == null) {
            return;
        }
        if (rest.size() == 1) {
            if (actor.isPlayer() && core.platform().gui() != null) {
                core.platform().gui().openPlayer(actor, target);
                return;
            }
            send(actor, "template.list-header", "player", target.name());
            for (TemplateService.Template t : core.templates().all()) {
                if (!core.templates().canUse(actor, t)) {
                    continue;
                }
                TemplateService.NextStep next = core.templates().next(t, target.uuid());
                String line = core.messages().get("template.list-line", "id", t.id(), "name", t.name(),
                        "type", core.formatter().typeName(next.step().type()), "duration", core.formatter().stepDuration(next.step().type(), next.step().duration()),
                        "step", next.index() + 1, "steps", t.steps().size());
                actor.sendClickable(line, CommandActor.ClickAction.RUN_COMMAND, "/punish " + target.name() + " " + t.id(),
                        core.messages().get("template.click-hover"));
            }
            return;
        }
        TemplateService.Template template = core.templates().get(rest.get(1));
        if (template == null) {
            send(actor, "template.unknown", "template", rest.get(1));
            return;
        }
        int start = 2;
        Integer level = null;
        if (rest.size() > start && rest.get(start).matches("(?i)(nivel|level):.*")) {
            try {
                level = Integer.parseInt(rest.get(start++).split(":", 2)[1]) - 1;
            } catch (NumberFormatException e) {
                send(actor, "gui.route.invalid-level"); return;
            }
        }
        apply(core, actor, target, template, silentFlag, level, String.join(" ", rest.subList(start, rest.size())), null);
    }

    /** Applies the next step of a template; shared with the menus and web panel. */
    public static Outcome apply(BanovaCore core, CommandActor actor, PlayerRecord target, TemplateService.Template template, boolean silent) {
        return apply(core, actor, target, template, silent, null, "", null);
    }

    public static Outcome apply(BanovaCore core, CommandActor actor, PlayerRecord target, TemplateService.Template template,
                                boolean silent, Integer manualIndex, String context, Integer reviewedIndex) {
        if (!actor.hasPermission("banova.punish") || !core.templates().canUse(actor, template)) {
            actor.send(core.messages().get("error.no-permission"));
            return null;
        }
        TemplateService.NextStep next = core.templates().next(template, target.uuid());
        int index = manualIndex == null ? next.index() : manualIndex;
        if (index < 0 || index >= template.steps().size()) {
            actor.send(core.messages().get("gui.route.invalid-level")); return null;
        }
        if (!template.equals(core.templates().get(template.id())) || reviewedIndex != null && reviewedIndex != index) {
            actor.send(core.messages().get("gui.route.changed")); return null;
        }
        if (manualIndex != null && !actor.hasPermission("banova.route.level")) {
            actor.send(core.messages().get("error.no-permission")); return null;
        }
        if (!context.isBlank() && (!actor.hasPermission("banova.evidence") || context.length() > 1000)) {
            actor.send(core.messages().get("gui.route.context-invalid")); return null;
        }
        var step = template.step(index);
        if (!TemplateService.canApplyStep(actor, step, manualIndex != null)) {
            actor.send(core.messages().get("error.no-permission")); return null;
        }
        PunishmentService.Request req = PunishmentService.Request.of(step.type(), target, actor.ref());
        req.duration = step.duration();
        req.ipBased = step.ip();
        req.context = context;
        req.reason = template.reason();
        req.template = template.id();
        req.templateStep = index;
        req.silent = silent || template.silent() || core.config().getBoolean("punishments.silent-by-default", false);
        if (step.type() == dev.banova.api.PunishmentType.KICK && core.platform().player(target.uuid()) == null) {
            actor.send(core.messages().get("error.not-online", "player", target.name()));
            return null;
        }
        Outcome error = core.punishments().check(actor, req, manualIndex == null);
        if (error != null) {
            actor.send(core.messages().get(error.errorKey(), error.errorArgs()));
            return error;
        }
        Outcome outcome = core.punishments().create(req);
        if (!outcome.ok()) {
            actor.send(core.messages().get(outcome.errorKey(), outcome.errorArgs()));
            return outcome;
        }
        actor.send(core.messages().get("template.applied", "template", template.name(), "step", index + 1,
                "steps", template.steps().size(), "player", target.name(), "type", core.formatter().typeName(step.type()),
                "duration", core.formatter().stepDuration(step.type(), step.duration()), "id", outcome.punishment().id));
        return outcome;
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            return onlineNames(args[0]);
        }
        if (args.length == 2) {
            List<String> ids = new ArrayList<>();
            for (TemplateService.Template t : core.templates().all()) {
                if (core.templates().canUse(actor, t)) {
                    ids.add(t.id());
                    ids.addAll(t.aliases());
                }
            }
            return filter(ids, args[1]);
        }
        return Collections.emptyList();
    }
}
