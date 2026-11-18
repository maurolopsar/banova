package dev.banova.bukkit.listener;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public final class RecentChat {
    public record Line(long time, String text) {}

    private final LinkedHashMap<UUID, ArrayDeque<Line>> messages = new LinkedHashMap<>(32, 0.75f, true);

    public synchronized void record(UUID sender, String text) {
        var lines = messages.computeIfAbsent(sender, ignored -> new ArrayDeque<>());
        lines.addFirst(new Line(System.currentTimeMillis(), text));
        if (lines.size() > 30) lines.removeLast();
        if (messages.size() > 512) messages.remove(messages.keySet().iterator().next());
    }

    public synchronized List<Line> forPlayer(UUID sender) {
        var lines = messages.get(sender);
        if (lines == null) return List.of();
        long cutoff = System.currentTimeMillis() - 30 * 60_000L;
        return lines.stream().filter(line -> line.time() >= cutoff).toList();
    }
}
