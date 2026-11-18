package dev.banova.velocity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * {@code &} color codes to Adventure components.
 */
final class VelocityText {

    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder().character('&').hexColors().build();

    private VelocityText() {
    }

    static Component of(String text) {
        return AMP.deserialize(text == null ? "" : text);
    }

    static Component clickable(String text, String action, String value, String hover) {
        ClickEvent click = switch (action) {
            case "RUN_COMMAND" -> ClickEvent.runCommand(value);
            case "OPEN_URL" -> ClickEvent.openUrl(value);
            case "COPY" -> ClickEvent.copyToClipboard(value);
            default -> ClickEvent.suggestCommand(value);
        };
        Component c = of(text).clickEvent(click);
        if (hover != null && !hover.isEmpty()) {
            c = c.hoverEvent(HoverEvent.showText(of(hover)));
        }
        return c;
    }
}
