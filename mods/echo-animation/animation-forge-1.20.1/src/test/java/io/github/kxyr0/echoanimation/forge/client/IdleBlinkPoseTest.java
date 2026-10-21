package io.github.kxyr0.echoanimation.forge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceControlId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec2;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.definition.resource.FaceControlDefinition;
import io.github.kxyr0.echoanimation.forge.face.BlinkTimingOverride;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class IdleBlinkPoseTest {
    private static final BoneId UPPER = BoneId.parse("upper_eyelid");
    private static final BoneId LOWER = BoneId.parse("lower_eyelid");

    @Test
    void timingOverrideChangesIdleBlinkWindow() {
        IdleBlinkPose pose = new IdleBlinkPose();
        FaceControlDefinition control = control(80, 1, 0, 1);

        BonePose base = pose.apply(List.of(control), new BonePose(Map.of()), null, 3.0D);
        BonePose overridden = pose.apply(List.of(control), new BonePose(Map.of()), null, 3.0D,
                Optional.of(new BlinkTimingOverride(12, 2, 4, 2)));

        assertTrue(base.transform(UPPER).isEmpty());
        assertEquals(0.035D, overridden.transform(UPPER).orElseThrow().translation().y(), 0.0001D);
        assertEquals(-0.035D, overridden.transform(LOWER).orElseThrow().translation().y(), 0.0001D);
    }

    private static FaceControlDefinition control(int period, int close, int hold, int open) {
        return new FaceControlDefinition(FaceControlId.parse("eye_closure"),
                0.035D, -0.035D, Vec2.ZERO, new Vec2(1.0D, 1.0D), UPPER, LOWER, true,
                period, close, hold, open);
    }
}
