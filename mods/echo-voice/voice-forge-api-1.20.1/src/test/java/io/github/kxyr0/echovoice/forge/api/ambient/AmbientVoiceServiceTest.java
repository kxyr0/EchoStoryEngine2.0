package io.github.kxyr0.echovoice.forge.api.ambient;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AmbientVoiceServiceTest {
    private final VoiceAssetKey asset = VoiceAssetKey.of(DialogueId.parse("jdh:hallway_talk"), "line_01");

    @Test
    void acceptsFiniteBoundedSpatialRequest() {
        assertDoesNotThrow(() -> new AmbientVoiceService.SpatialRequest(
                "ambient/session/1", asset, 1.0D, 64.0D, -2.0D, 14.0D, 0.8D));
    }

    @Test
    void rejectsInvalidTokenCoordinatesDistanceAndVolume() {
        assertThrows(IllegalArgumentException.class, () -> request(" ", 0.0D, 14.0D, 0.8D));
        assertThrows(IllegalArgumentException.class, () -> request("token", Double.NaN, 14.0D, 0.8D));
        assertThrows(IllegalArgumentException.class, () -> request("token", 0.0D, 65.0D, 0.8D));
        assertThrows(IllegalArgumentException.class, () -> request("token", 0.0D, 14.0D, 1.1D));
    }

    private AmbientVoiceService.SpatialRequest request(String token, double x, double range, double volume) {
        return new AmbientVoiceService.SpatialRequest(token, asset, x, 64.0D, 0.0D, range, volume);
    }
}
