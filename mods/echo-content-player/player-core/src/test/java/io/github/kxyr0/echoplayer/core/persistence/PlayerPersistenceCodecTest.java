package io.github.kxyr0.echoplayer.core.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import org.junit.jupiter.api.Test;

final class PlayerPersistenceCodecTest {
    @Test
    void persistenceDtoRoundTrips() {
        PlayerProfileData data = new PlayerProfileData("11111111-1111-1111-1111-111111111111",
                "Account", "CUSTOM", "Echo", 70, 100, "RESOURCE", "jdh:player/default",
                "SLIM", "skinhash", "jdh:player/default", "abc", 5L, 9L, 2L,
                "2026-07-25T00:00:00Z", "2026-07-25T01:00:00Z", "",
                "Account", "Account", true);

        PlayerPersistenceCodec codec = new PlayerPersistenceCodec();

        assertEquals(data, codec.decode(codec.encode(data)));
        PlayerProfileData normalized = PlayerProfileData.fromSnapshot(data.toSnapshot());
        assertEquals("echo_content_player:player/base", normalized.skinId());
        assertEquals(PlayerBaseSkin.MODEL.name(), normalized.skinModel());
        assertEquals(PlayerBaseSkin.FINGERPRINT, normalized.skinFingerprint());
    }
}
