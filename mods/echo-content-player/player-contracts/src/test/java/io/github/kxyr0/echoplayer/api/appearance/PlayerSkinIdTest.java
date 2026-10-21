package io.github.kxyr0.echoplayer.api.appearance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class PlayerSkinIdTest {
    @Test
    void resourceSkinIdParses() {
        PlayerSkinId skinId = PlayerSkinId.parse("jdh:player/default");

        assertEquals("jdh:player/default", skinId.value());
        assertEquals("textures/echo_player/skins/player/default.png", skinId.resourcePath());
    }

    @Test
    void traversalAndUppercaseFail() {
        assertThrows(IllegalArgumentException.class, () -> PlayerSkinId.parse("jdh:../bad"));
        assertThrows(IllegalArgumentException.class, () -> PlayerSkinId.parse("JDH:player/default"));
    }
}
