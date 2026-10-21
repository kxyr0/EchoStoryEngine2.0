package io.github.kxyr0.echostoryanimationadapter.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echostoryanimationadapter.action.AnimationActionPayload.AwaitMode;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class AnimationActionPayloadTest {
    @Test
    void decodesRequiredAnimationAndTarget() {
        AnimationActionPayload payload = AnimationActionPayload.decode(Map.of(
                "animation", "jdh:wave",
                "target", "jdh:john_father"), true);

        assertEquals("jdh:wave", payload.animationId().toString());
        assertEquals("jdh:john_father", payload.subjectId().toString());
        assertEquals(AwaitMode.STARTED, payload.awaitMode());
    }

    @Test
    void decodesExplicitCompletedAwait() {
        AnimationActionPayload payload = AnimationActionPayload.decode(Map.of(
                "animation", "jdh:wave_once",
                "target", "jdh:john_father",
                "await", "COMPLETED"), true);

        assertEquals(AwaitMode.COMPLETED, payload.awaitMode());
    }

    @Test
    void rejectsMissingTarget() {
        assertThrows(IllegalArgumentException.class, () -> AnimationActionPayload.decode(Map.of(
                "animation", "jdh:wave"), true));
    }
}
