package dev.banova.core.filter;

import dev.banova.core.BanovaCore;
import dev.banova.core.config.YamlConfig;
import dev.banova.core.model.ActorRef;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.platform.OnlinePlayer;
import dev.banova.core.service.PunishmentService;
import dev.banova.core.service.TemplateService;
import dev.banova.core.util.Durations;
import dev.banova.core.util.Text;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Chat filter: blocked words (with leetspeak/accents/spacing tricks handled), advertising,
 * spam, caps and character flood. Repeated violations can trigger an escalation template.
 */
public final class ChatFilter {

    public enum Action {
        ALLOW,
        MODIFY,
        BLOCK
    }

    public record Result(Action action, String message, String rule) {

        static Result allow(String message) {
            return new Result(Action.ALLOW, message, null);
        }
    }

    private record CustomRule(String name, Pattern pattern, boolean block) {
    }

    private static final Pattern IP = Pattern.compile("\\b\\d{1,3}\\s*[.,]\\s*\\d{1,3}\\s*[.,]\\s*\\d{1,3}\\s*[.,]\\s*\\d{1,3}(\\s*:\\s*\\d{2,5})?\\b");
    private static final Pattern DOMAIN = Pattern.compile(
            "(?i)\\b([a-z0-9][a-z0-9-]{1,62})\\s*(\\.|\\(dot\\)|\\[dot\\]|\\(punto\\)|\\[punto\\]|\\s+dot\\s+|\\s+punto\\s+)\\s*"
                    + "(com|net|org|es|me|gg|io|xyz|tk|ml|ga|cf|us|co|eu|ru|de|fr|br|mx|ar|cl|pe|uk|info|biz|club|online|site|fun|pro|top|"
                    + "store|shop|link|live|tv|cc|pw|ly|to|nu|ws|be|nl|it|pl|ch|at|host|games|play|network|mc|land|world)\\b");

    private final BanovaCore core;
    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();

    private boolean enabled;
    private boolean wordsEnabled;
    private boolean wordsBlock;
    private Set<String> words = Set.of();
    private Set<String> whitelist = Set.of();
    private boolean adsEnabled;
    private boolean adsBlock;
    private List<String> adsWhitelist = List.of();
    private boolean spamEnabled;
    private long minDelay;
    private double repeatThreshold;
    private int repeatHistory;
    private boolean capsEnabled;
    private int capsMinLength;
    private int capsMaxPercent;
    private boolean floodEnabled;
    private int floodMax;
    private List<CustomRule> customRules = List.of();
    private boolean autoPunish;
    private int autoViolations;
    private long autoWindow;
    private String autoTemplate;
    private boolean notifyStaff;

    public ChatFilter(BanovaCore core) {
        this.core = core;
    }

    public void load(YamlConfig cfg) {
        enabled = cfg.getBoolean("enabled", true);
        notifyStaff = cfg.getBoolean("notify-staff", true);

        wordsEnabled = cfg.getBoolean("words.enabled", true);
        wordsBlock = cfg.getString("words.action", "censor").equalsIgnoreCase("block");
        Set<String> w = new HashSet<>();
        for (String word : cfg.getStringList("words.list")) {
            String n = collapse(normalize(word));
            if (!n.isEmpty()) {
                w.add(n);
            }
        }
        words = w;
        Set<String> wl = new HashSet<>();
        for (String word : cfg.getStringList("words.whitelist")) {
            wl.add(collapse(normalize(word)));
        }
        whitelist = wl;

        adsEnabled = cfg.getBoolean("advertising.enabled", true);
        adsBlock = !cfg.getString("advertising.action", "block").equalsIgnoreCase("censor");
        List<String> domains = new ArrayList<>();
        for (String d : cfg.getStringList("advertising.whitelist")) {
            domains.add(d.toLowerCase(Locale.ROOT).trim());
        }
        adsWhitelist = domains;

        spamEnabled = cfg.getBoolean("spam.enabled", true);
        minDelay = Math.max(0, cfg.getLong("spam.min-delay-ms", 800));
        repeatThreshold = Math.min(1.0, Math.max(0.5, cfg.getDouble("spam.similarity", 0.85)));
        repeatHistory = Math.max(1, cfg.getInt("spam.history", 3));

        capsEnabled = cfg.getBoolean("caps.enabled", true);
        capsMinLength = Math.max(1, cfg.getInt("caps.min-length", 8));
        capsMaxPercent = Math.max(1, Math.min(100, cfg.getInt("caps.max-percent", 70)));

        floodEnabled = cfg.getBoolean("flood.enabled", true);
        floodMax = Math.max(2, cfg.getInt("flood.max-repeated-chars", 4));

        List<CustomRule> rules = new ArrayList<>();
        for (YamlConfig rule : cfg.getSectionList("custom")) {
            String pattern = rule.getString("pattern", "");
            if (pattern.isBlank()) {
                continue;
            }
            try {
                rules.add(new CustomRule(rule.getString("name", "custom"), Pattern.compile(pattern, Pattern.CASE_INSENSITIVE),
                        !rule.getString("action", "block").equalsIgnoreCase("censor")));
            } catch (PatternSyntaxException e) {
                core.platform().logger().warn("filter.yml: invalid regex '" + pattern + "': " + e.getDescription());
            }
        }
        customRules = rules;

        autoPunish = cfg.getBoolean("auto-punish.enabled", true);
        autoViolations = Math.max(1, cfg.getInt("auto-punish.violations", 3));
        autoWindow = Math.max(1000, Durations.parse(cfg.getString("auto-punish.window", "10m")));
        autoTemplate = cfg.getString("auto-punish.template", "chat");
    }

    public boolean enabled() {
        return enabled;
    }

    /** Template applied automatically after repeated violations, or null when auto-punish is off. */
    public String autoTemplate() {
        return autoPunish ? autoTemplate : null;
    }

    public void forget(UUID player) {
        states.remove(player);
    }

    /**
     * Checks a chat message. Safe to call from async chat threads.
     */
    public Result check(OnlinePlayer player, String original) {
        if (!enabled || player.hasPermission("banova.filter.bypass")) {
            return Result.allow(original);
        }
        PlayerState state = states.computeIfAbsent(player.uuid(), id -> new PlayerState());
        String message = original;
        boolean modified = false;

        // 1) Spam: too fast or repeated
        if (spamEnabled) {
            long now = System.currentTimeMillis();
            synchronized (state) {
                if (now - state.lastMessage < minDelay) {
                    state.lastMessage = now;
                    return block(player, state, original, "spam-fast", false);
                }
                String norm = collapse(normalize(original));
                if (norm.length() >= 3) {
                    for (String previous : state.history) {
                        if (similarity(previous, norm) >= repeatThreshold) {
                            state.lastMessage = now;
                            return block(player, state, original, "spam-repeat", true);
                        }
                    }
                }
                state.lastMessage = now;
                state.history.addFirst(norm);
                while (state.history.size() > repeatHistory) {
                    state.history.removeLast();
                }
            }
        }

        // 2) Advertising
        if (adsEnabled) {
            String found = findAdvertising(message);
            if (found != null) {
                if (adsBlock) {
                    return block(player, state, original, "advertising", true);
                }
                message = message.replace(found, "*".repeat(found.length()));
                modified = true;
            }
        }

        // 3) Custom regex rules
        for (CustomRule rule : customRules) {
            Matcher m = rule.pattern().matcher(message);
            if (m.find()) {
                if (rule.block()) {
                    return block(player, state, original, "custom:" + rule.name(), true);
                }
                message = rule.pattern().matcher(message).replaceAll(r -> "*".repeat(r.group().length()));
                modified = true;
            }
        }

        // 4) Blocked words
        if (wordsEnabled && !words.isEmpty()) {
            WordCheck wc = checkWords(message);
            if (wc.hit) {
                if (wordsBlock || wc.hiddenHit) {
                    return block(player, state, original, "words", true);
                }
                message = wc.censored;
                modified = true;
                violation(player, state, original, "words");
            }
        }

        // 5) Flood (repeated characters) and caps: silently fixed
        if (floodEnabled) {
            String fixed = limitRepeats(message, floodMax);
            if (!fixed.equals(message)) {
                message = fixed;
                modified = true;
            }
        }
        if (capsEnabled && tooManyCaps(message)) {
            message = lowerCaseKeepingNames(message);
            modified = true;
        }
        return modified ? new Result(Action.MODIFY, message, null) : Result.allow(message);
    }

    // ------------------------------------------------------------------ violations

    private Result block(OnlinePlayer player, PlayerState state, String original, String rule, boolean countViolation) {
        String key = rule.startsWith("custom:") ? "custom" : rule;
        player.send(core.messages().get("filter.blocked-" + key));
        if (countViolation) {
            violation(player, state, original, rule);
        }
        return new Result(Action.BLOCK, original, rule);
    }

    private void violation(OnlinePlayer player, PlayerState state, String original, String rule) {
        if (notifyStaff) {
            core.platform().broadcast(core.messages().get("filter.staff-alert", "player", player.name(), "rule", rule,
                    "message", Text.escapeColors(Text.truncate(original, 120))), "banova.notify.filter");
        }
        if (!autoPunish) {
            return;
        }
        boolean punish;
        synchronized (state) {
            long now = System.currentTimeMillis();
            state.violations.addLast(now);
            while (!state.violations.isEmpty() && now - state.violations.peekFirst() > autoWindow) {
                state.violations.removeFirst();
            }
            punish = state.violations.size() >= autoViolations;
            if (punish) {
                state.violations.clear();
            }
        }
        if (punish) {
            UUID uuid = player.uuid();
            String name = player.name();
            String ip = player.ip();
            core.async(() -> autoPunish(uuid, name, ip, rule));
        }
    }

    private void autoPunish(UUID uuid, String name, String ip, String rule) {
        TemplateService.Template template = core.templates().get(autoTemplate);
        if (template == null) {
            core.platform().logger().warn("filter.yml auto-punish template '" + autoTemplate + "' does not exist in templates.yml");
            return;
        }
        TemplateService.NextStep next = core.templates().next(template, uuid);
        PunishmentService.Request req = PunishmentService.Request.of(next.step().type(), new PlayerRecord(uuid, name, ip, 0, 0), ActorRef.SYSTEM);
        req.duration = next.step().duration();
        req.ipBased = next.step().ip();
        req.reason = template.reason() + " (" + core.messages().raw("filter.auto-reason") + ": " + rule + ")";
        req.template = template.id();
        req.templateStep = next.index();
        req.silent = template.silent();
        core.punishments().create(req);
    }

    // ------------------------------------------------------------------ words

    private record WordCheck(boolean hit, boolean hiddenHit, String censored) {
    }

    private WordCheck checkWords(String message) {
        String[] tokens = message.split(" ", -1);
        boolean hit = false;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            String norm = collapse(normalize(token));
            boolean bad = !norm.isEmpty() && words.contains(norm) && !whitelist.contains(norm);
            if (bad) {
                hit = true;
                out.append("*".repeat(Math.max(1, token.length())));
            } else {
                out.append(token);
            }
            if (i < tokens.length - 1) {
                out.append(' ');
            }
        }
        // Words written letter by letter ("i d i o t a") are treated as an evasion attempt.
        boolean hidden = false;
        StringBuilder run = new StringBuilder();
        for (String token : tokens) {
            String n = normalize(token);
            if (n.length() == 1) {
                run.append(n);
                continue;
            }
            hidden |= runHits(run);
            run.setLength(0);
        }
        hidden |= runHits(run);
        return new WordCheck(hit || hidden, hidden, out.toString());
    }

    private boolean runHits(StringBuilder run) {
        if (run.length() < 3) {
            return false;
        }
        String joined = collapse(run.toString());
        return words.contains(joined) && !whitelist.contains(joined);
    }

    /** Lower case, no accents, common leetspeak replaced, only letters and digits kept. */
    static String normalize(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        // Punctuation around a word ("idiot!", "(idiot)") is not leetspeak: only symbols inside a word are.
        int start = 0;
        int end = noAccents.length();
        while (start < end && !Character.isLetterOrDigit(noAccents.charAt(start)) && noAccents.charAt(start) != '@'
                && noAccents.charAt(start) != '$') {
            start++;
        }
        while (end > start && !Character.isLetterOrDigit(noAccents.charAt(end - 1))) {
            end--;
        }
        noAccents = noAccents.substring(start, end);
        StringBuilder sb = new StringBuilder(noAccents.length());
        for (char c : noAccents.toCharArray()) {
            char mapped = switch (c) {
                case '4', '@' -> 'a';
                case '3' -> 'e';
                case '1', '!', '|' -> 'i';
                case '0' -> 'o';
                case '5', '$' -> 's';
                case '7' -> 't';
                default -> c;
            };
            if (Character.isLetterOrDigit(mapped)) {
                sb.append(mapped);
            }
        }
        return sb.toString();
    }

    /** "idiooootaaa" -> "idiota". */
    static String collapse(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        char last = 0;
        for (char c : text.toCharArray()) {
            if (c != last) {
                sb.append(c);
            }
            last = c;
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ advertising

    private String findAdvertising(String message) {
        Matcher ip = IP.matcher(message);
        while (ip.find()) {
            String found = ip.group();
            String digits = found.replaceAll("\\s", "").replace(',', '.');
            if (!adsWhitelist.contains(digits.split(":")[0])) {
                return found;
            }
        }
        Matcher domain = DOMAIN.matcher(message);
        while (domain.find()) {
            String found = domain.group();
            String clean = (domain.group(1) + "." + domain.group(3)).toLowerCase(Locale.ROOT);
            boolean allowed = false;
            for (String w : adsWhitelist) {
                if (clean.equals(w) || clean.endsWith("." + w) || w.endsWith("." + clean)) {
                    allowed = true;
                    break;
                }
            }
            // Also allow subdomains like play.yourserver.com where the matched part is the main domain.
            if (!allowed) {
                return found;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ caps & flood

    private boolean tooManyCaps(String message) {
        int letters = 0;
        int upper = 0;
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    upper++;
                }
            }
        }
        return letters >= capsMinLength && upper * 100 > letters * capsMaxPercent;
    }

    private String lowerCaseKeepingNames(String message) {
        Set<String> names = new HashSet<>();
        for (OnlinePlayer p : core.platform().onlinePlayers()) {
            names.add(p.name());
        }
        String[] tokens = message.split(" ", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            sb.append(names.contains(tokens[i]) ? tokens[i] : tokens[i].toLowerCase(Locale.ROOT));
            if (i < tokens.length - 1) {
                sb.append(' ');
            }
        }
        String out = sb.toString();
        return out.isEmpty() ? out : Character.toUpperCase(out.charAt(0)) + out.substring(1);
    }

    static String limitRepeats(String text, int max) {
        StringBuilder sb = new StringBuilder(text.length());
        char last = 0;
        int run = 0;
        for (char c : text.toCharArray()) {
            if (c == last) {
                run++;
            } else {
                run = 1;
                last = c;
            }
            if (run <= max || c == '*') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Normalized Levenshtein similarity between 0 and 1. */
    static double similarity(String a, String b) {
        if (a.equals(b)) {
            return 1.0;
        }
        int max = Math.max(a.length(), b.length());
        if (max == 0) {
            return 1.0;
        }
        if (Math.abs(a.length() - b.length()) > max / 2) {
            return 0;
        }
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return 1.0 - (double) prev[b.length()] / max;
    }

    private static final class PlayerState {
        long lastMessage;
        final Deque<String> history = new ArrayDeque<>();
        final Deque<Long> violations = new ArrayDeque<>();
    }
}
