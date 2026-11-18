package dev.banova.core.service;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.config.YamlConfig;
import dev.banova.core.util.Durations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Punishment templates with automatic escalation ("ladders").
 * Each repeated offense of the same template moves the player one step up.
 */
public final class TemplateService {

    public record Step(PunishmentType type, long duration, boolean ip) {
    }

    public enum Category {
        CHAT, GAMEPLAY, ACCOUNT;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Template(String id, String name, String icon, Category category, String reason, String permission, long resetAfter,
                           boolean silent, List<String> aliases, List<Step> steps) {

        public Step step(int index) {
            return steps.get(Math.max(0, Math.min(index, steps.size() - 1)));
        }
    }

    /** The step a player would receive next, with how many previous offenses were counted. */
    public record NextStep(Template template, int index, Step step, long offenses) {
    }

    private final BanovaCore core;
    private volatile Map<String, Template> templates = Collections.emptyMap();

    public TemplateService(BanovaCore core) {
        this.core = core;
    }

    public void load(YamlConfig file) {
        Map<String, Template> loaded = new LinkedHashMap<>();
        YamlConfig section = file.section("templates");
        for (String id : section.keys("")) {
            YamlConfig t = section.section(id);
            List<Step> steps = new ArrayList<>();
            for (YamlConfig s : t.getSectionList("steps")) {
                PunishmentType type = PunishmentType.parse(s.getString("type", "ban"));
                if (type == null) {
                    core.platform().logger().warn("templates.yml: unknown type '" + s.getString("type") + "' in template " + id);
                    continue;
                }
                long duration = Durations.parse(s.getString("duration", "perm"));
                if (duration < 0) {
                    core.platform().logger().warn("templates.yml: invalid duration '" + s.getString("duration") + "' in template " + id);
                    continue;
                }
                steps.add(new Step(type, duration, s.getBoolean("ip", false)));
            }
            if (steps.isEmpty()) {
                core.platform().logger().warn("templates.yml: template " + id + " has no valid steps, skipped");
                continue;
            }
            long reset = Durations.parse(t.getString("reset-after", "perm"));
            String key = id.toLowerCase(Locale.ROOT);
            String group = t.getString("category", "account");
            Category category;
            try {
                category = Category.valueOf(group.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                core.platform().logger().warn("templates.yml: unknown category '" + group + "' in " + id + ", using account");
                category = Category.ACCOUNT;
            }
            loaded.put(key, new Template(key, t.getString("name", id), t.getString("icon", "PAPER"), category,
                    t.getString("reason", t.getString("name", id)), t.getString("permission", ""),
                    Math.max(0, reset), t.getBoolean("silent", false), List.copyOf(t.getStringList("aliases")), List.copyOf(steps)));
        }
        templates = loaded;
    }

    public Collection<Template> all() {
        return templates.values();
    }

    public Template get(String id) {
        if (id == null) return null;
        Template exact = templates.get(id.toLowerCase(Locale.ROOT));
        if (exact != null) return exact;
        return all().stream().filter(route -> route.aliases().stream().anyMatch(id::equalsIgnoreCase)).findFirst().orElse(null);
    }

    public NextStep next(Template template, UUID target) {
        long since = template.resetAfter() <= 0 ? 0 : System.currentTimeMillis() - template.resetAfter();
        if (target == null) return new NextStep(template, 0, template.step(0), 0);
        var progress = core.punishmentRepo().progress(target, template.id(), since);
        int index = Math.min(progress.highestIndex() + 1, template.steps().size() - 1);
        return new NextStep(template, index, template.step(index), progress.offenses());
    }

    public Map<String, NextStep> previews(Collection<Template> routes, UUID player) {
        long now = System.currentTimeMillis();
        Map<String, Long> cutoffs = new LinkedHashMap<>();
        for (Template route : routes) {
            cutoffs.put(route.id(), route.resetAfter() > 0 ? now - route.resetAfter() : 0);
        }
        var progress = core.punishmentRepo().progress(player, cutoffs);
        Map<String, NextStep> result = new LinkedHashMap<>();
        for (Template route : routes) {
            var previous = progress.get(route.id());
            int index = previous == null ? 0 : Math.min(previous.highestIndex() + 1, route.steps().size() - 1);
            result.put(route.id(), new NextStep(route, index, route.step(index), previous == null ? 0 : previous.offenses()));
        }
        return result;
    }

    public static boolean canApplyStep(dev.banova.core.platform.CommandActor actor, Step step, boolean manual) {
        return !manual && step.type() != PunishmentType.BAN
                || PunishmentService.canCreate(actor, step.type(), step.duration() > 0, step.ip());
    }

    public boolean canUse(dev.banova.core.platform.CommandActor actor, Template template) {
        String perm = template.permission();
        return perm == null || perm.isBlank() || actor.hasPermission(perm);
    }
}
