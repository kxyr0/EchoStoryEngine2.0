package io.github.kxyr0.echoobservation.forge.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObservationStoryPlayerContextTest {
    @Test
    void resolvesOnlyExplicitOrInheritedCurrentPlayer() {
        UUID inherited = UUID.randomUUID();
        UUID explicit = UUID.randomUUID();
        assertEquals(inherited, ObservationStoryPlayerContext.resolve(
                "jdh:execution/player/" + inherited + "/chapter/1", Map.of()).orElseThrow());
        assertEquals(explicit, ObservationStoryPlayerContext.resolve(
                "jdh:execution/player/" + inherited, Map.of("player", explicit.toString())).orElseThrow());
        assertTrue(ObservationStoryPlayerContext.resolve("jdh:unowned_execution", Map.of()).isEmpty());
    }
}
