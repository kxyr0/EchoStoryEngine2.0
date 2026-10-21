package io.github.kxyr0.echoplayer.api.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class PlayerDisplayNameTest {
    @Test
    void customNameTrimsAndAllowsUnicode() {
        assertEquals("Искра", PlayerDisplayName.of("  Искра  ").value());
    }

    @Test
    void emptySpacesAndControlCharactersFail() {
        assertThrows(IllegalArgumentException.class, () -> PlayerDisplayName.of(""));
        assertThrows(IllegalArgumentException.class, () -> PlayerDisplayName.of("   "));
        assertThrows(IllegalArgumentException.class, () -> PlayerDisplayName.of("Bad\nName"));
    }

    @Test
    void longerThanFortyEightCodePointsFails() {
        String name = "а".repeat(49);

        assertThrows(IllegalArgumentException.class, () -> PlayerDisplayName.of(name));
    }
}
