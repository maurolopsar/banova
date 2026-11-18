package dev.banova.core.util;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextTest {

    @Test
    void detectsIps() {
        assertTrue(Text.isIp("192.168.1.20"));
        assertTrue(Text.isIp("2001:db8::1"));
        assertFalse(Text.isIp("999.1.1.1"));
        assertFalse(Text.isIp("Notch"));
    }

    @Test
    void parsesUuidsWithAndWithoutDashes() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, Text.parseUuid(uuid.toString()));
        assertEquals(uuid, Text.parseUuid(uuid.toString().replace("-", "")));
        assertNull(Text.parseUuid("Notch"));
    }

    @Test
    void replacesPlaceholders() {
        assertEquals("Hi Steve, 7d", Text.replace("Hi {player}, {duration}", Map.of("player", "Steve", "duration", "7d")));
    }

    @Test
    void masksIp() {
        assertEquals("10.20.*.*", Text.maskIp("10.20.30.40"));
    }

    @Test
    void humanCodesNormalize() {
        assertEquals("K7M2-QX9P", Codes.normalizeHumanCode(" k7m2qx9p "));
        assertEquals(9, Codes.humanCode().length());
    }
}
