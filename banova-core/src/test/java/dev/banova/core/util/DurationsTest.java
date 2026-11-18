package dev.banova.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationsTest {

    @Test
    void parsesUnitsAndCombinations() {
        assertEquals(30_000L, Durations.parse("30s"));
        assertEquals(15 * 60_000L, Durations.parse("15m"));
        assertEquals(15 * 60_000L, Durations.parse("15min"));
        assertEquals(2 * 3_600_000L, Durations.parse("2h"));
        assertEquals(7 * 86_400_000L, Durations.parse("7d"));
        assertEquals(14 * 86_400_000L, Durations.parse("2w"));
        assertEquals(30 * 86_400_000L, Durations.parse("1mo"));
        assertEquals(86_400_000L + 12 * 3_600_000L, Durations.parse("1d12h"));
        assertEquals(86_400_000L, Durations.parse("1D"));
    }

    @Test
    void permanentWords() {
        for (String word : new String[]{"perm", "permanent", "permanente", "forever", "-1"}) {
            assertEquals(Durations.PERMANENT, Durations.parse(word), word);
        }
    }

    @Test
    void rejectsText() {
        assertEquals(-1, Durations.parse("griefing"));
        assertEquals(-1, Durations.parse("0"));
        assertEquals(-1, Durations.parse(""));
        assertEquals(-1, Durations.parse(null));
        assertEquals(-1, Durations.parse("5x"));
        assertFalse(Durations.isDuration("hacks"));
        assertTrue(Durations.isDuration("perm"));
    }

    @Test
    void formatsWithTwoUnits() {
        assertEquals("7d", Durations.format(7 * 86_400_000L));
        assertEquals("1d 12h", Durations.format(86_400_000L + 12 * 3_600_000L));
        assertEquals("45m", Durations.format(45 * 60_000L));
        assertEquals("1mo", Durations.format(30 * 86_400_000L));
    }

    @Test
    void inputFormatRoundTrips() {
        long value = Durations.parse("3d4h5m");
        assertEquals(value, Durations.parse(Durations.toInput(value)));
        assertEquals("perm", Durations.toInput(0));
    }
}
