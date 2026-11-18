package dev.banova.core.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatFilterTextTest {

    @Test
    void normalizesLeetspeakAndAccents() {
        assertEquals("idiota", ChatFilter.collapse(ChatFilter.normalize("1d10t4")));
        assertEquals("estupido", ChatFilter.normalize("Estúpido"));
        assertEquals("idiota", ChatFilter.collapse(ChatFilter.normalize("IDIOOOTAAA!")));
    }

    @Test
    void limitsRepeatedCharactersButKeepsCensorStars() {
        assertEquals("hellooo", ChatFilter.limitRepeats("hellooooooo", 3));
        assertEquals("******", ChatFilter.limitRepeats("******", 3));
    }

    @Test
    void similarity() {
        assertEquals(1.0, ChatFilter.similarity("hola", "hola"));
        assertTrue(ChatFilter.similarity("compro diamantes", "compro diamante") > 0.85);
        assertTrue(ChatFilter.similarity("hola", "adios amigos") < 0.5);
    }
}
