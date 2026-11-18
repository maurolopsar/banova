package dev.banova.core.web;

import dev.banova.core.model.Records.WebSession;
import dev.banova.core.model.Source;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A web panel user acting like a command sender. Messages are collected and returned to the browser.
 */
final class WebActor implements CommandActor {

    private final WebSession session;
    private final List<String> messages = new ArrayList<>();

    WebActor(WebSession session) {
        this.session = session;
    }

    WebSession session() {
        return session;
    }

    @Override
    public UUID uuid() {
        return session.uuid();
    }

    @Override
    public String name() {
        return session.name();
    }

    @Override
    public boolean hasPermission(String permission) {
        return session.has(permission);
    }

    @Override
    public synchronized void send(String message) {
        for (String line : message.split("\n")) {
            String clean = Text.strip(line).trim();
            if (!clean.isEmpty()) {
                messages.add(clean);
            }
        }
    }

    @Override
    public Source source() {
        return Source.WEB;
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    synchronized List<String> messages() {
        return new ArrayList<>(messages);
    }
}
