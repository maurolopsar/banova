package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.Evidence;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Text;

import java.util.Collections;
import java.util.List;

/**
 * /evidence &lt;#id&gt; — list evidence of a punishment.
 * /evidence &lt;#id&gt; &lt;link or text&gt; — attach evidence.
 * /evidence remove &lt;evidenceId&gt; — remove one.
 */
public final class EvidenceCommand extends Command {

    public EvidenceCommand(BanovaCore core) {
        super(core, "evidence", "banova.evidence", "proof", "pruebas", "evidencia");
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        if (args.length < 1) {
            usage(actor);
            return;
        }
        if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("borrar")) && args.length > 1) {
            remove(core, actor, parseId(args[1]));
            return;
        }
        Punishment p = punishmentById(actor, args[0]);
        if (p == null) {
            return;
        }
        if (args.length == 1) {
            List<Evidence> list = core.records().evidence(p.id);
            if (list.isEmpty()) {
                send(actor, "evidence.empty", "id", p.id);
                return;
            }
            send(actor, "evidence.header", "id", p.id, "count", list.size());
            for (Evidence e : list) {
                String line = core.messages().get("evidence.line", "eid", e.id(), "actor", e.actorName(),
                        "date", core.formatter().date(e.createdAt()), "text", e.content());
                if (e.content().startsWith("http://") || e.content().startsWith("https://")) {
                    actor.sendClickable(line, CommandActor.ClickAction.OPEN_URL, e.content(), core.messages().get("evidence.hover-open"));
                } else {
                    actor.send(line);
                }
            }
            return;
        }
        add(core, actor, p, Text.join(args, 1));
    }

    public static long add(BanovaCore core, CommandActor actor, Punishment p, String content) {
        String text = content.trim();
        if (text.length() > 1000) {
            text = text.substring(0, 1000);
        }
        long id = core.records().addEvidence(p.id, text, actor.ref());
        core.audit().log(actor.ref(), "EVIDENCE_ADD", p.targetUuid, p.targetName, p.id, text);
        actor.send(core.messages().get("evidence.added", "id", p.id, "eid", id));
        return id;
    }

    public static boolean remove(BanovaCore core, CommandActor actor, long evidenceId) {
        Evidence e = core.records().evidenceById(evidenceId);
        if (e == null) {
            actor.send(core.messages().get("evidence.not-found", "eid", evidenceId));
            return false;
        }
        boolean own = actor.uuid() != null && actor.uuid().equals(e.actorUuid());
        if (!own && !actor.hasPermission("banova.evidence.delete")) {
            actor.send(core.messages().get("error.no-permission"));
            return false;
        }
        core.records().deleteEvidence(evidenceId);
        Punishment p = core.punishments().byId(e.punishmentId());
        core.audit().log(actor.ref(), "EVIDENCE_DELETE", p == null ? null : p.targetUuid, p == null ? null : p.targetName,
                e.punishmentId(), e.content());
        actor.send(core.messages().get("evidence.removed", "eid", evidenceId));
        return true;
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            return filter(List.of("remove", "#"), args[0]);
        }
        return Collections.emptyList();
    }
}
