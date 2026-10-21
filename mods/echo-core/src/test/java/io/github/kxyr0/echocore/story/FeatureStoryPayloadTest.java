package io.github.kxyr0.echocore.story;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeatureStoryPayloadTest {
    @Test void validatesFlagTargetAndStrictBoolean() {
        assertTrue(FeatureStoryPayload.parse(Map.of("player", "@a", "flag", "magic_unlocked", "value", "true")).value());
        assertThrows(IllegalArgumentException.class, () -> FeatureStoryPayload.parse(Map.of("player", "@a", "flag", "magic_unlocked", "value", "yes")));
        assertThrows(IllegalArgumentException.class, () -> FeatureStoryPayload.parse(Map.of("flag", "magic_unlocked", "value", "true")));
    }
}
