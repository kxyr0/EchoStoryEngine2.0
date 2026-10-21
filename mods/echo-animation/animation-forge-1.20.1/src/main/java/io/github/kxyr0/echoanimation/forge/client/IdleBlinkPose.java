package io.github.kxyr0.echoanimation.forge.client;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.definition.resource.FaceControlDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.forge.face.BlinkTimingOverride;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adds configured idle eyelid closure on top of the current face pose.
 */
public final class IdleBlinkPose {
    public BonePose apply(FaceRigDefinition rig, BonePose pose, String subjectKey, double tick) {
        return apply(rig.controls(), pose, subjectKey, tick);
    }

    public BonePose apply(List<FaceControlDefinition> controls, BonePose pose, String subjectKey, double tick) {
        return apply(controls, pose, subjectKey, tick, Optional.empty());
    }

    public BonePose apply(List<FaceControlDefinition> controls, BonePose pose, String subjectKey, double tick,
            Optional<BlinkTimingOverride> timingOverride) {
        Map<BoneId, Transform3> transforms = new LinkedHashMap<>(pose.localTransforms());
        for (FaceControlDefinition control : controls) {
            FaceControlDefinition blinkControl = timingOverride.map(override -> override.apply(control))
                    .orElse(control);
            if (!blinkControl.idleBlink()) {
                continue;
            }
            double amount = amount(blinkControl, tick + offset(subjectKey, blinkControl.blinkPeriodTicks()));
            if (amount <= 0.0D) {
                continue;
            }
            addY(transforms, blinkControl.blinkUpperBone(), blinkControl.blinkUpperTravel() * amount);
            addY(transforms, blinkControl.blinkLowerBone(), blinkControl.blinkLowerTravel() * amount);
        }
        return new BonePose(transforms);
    }

    private void addY(Map<BoneId, Transform3> transforms, BoneId bone, double y) {
        Transform3 current = transforms.getOrDefault(bone, Transform3.IDENTITY);
        Vec3 translation = current.translation();
        transforms.put(bone, current.withTranslation(new Vec3(translation.x(), translation.y() + y,
                translation.z())));
    }

    private double amount(FaceControlDefinition control, double tick) {
        double phase = positiveModulo(tick, control.blinkPeriodTicks());
        double close = control.blinkCloseTicks();
        double holdEnd = close + control.blinkHoldTicks();
        double openEnd = holdEnd + control.blinkOpenTicks();
        if (phase < close) {
            return smooth(phase / close);
        }
        if (phase < holdEnd) {
            return 1.0D;
        }
        if (phase < openEnd) {
            return 1.0D - smooth((phase - holdEnd) / control.blinkOpenTicks());
        }
        return 0.0D;
    }

    private double offset(String subjectKey, int period) {
        return Math.floorMod(subjectKey == null ? 0 : subjectKey.hashCode(), period);
    }

    private double positiveModulo(double value, int period) {
        double result = value % period;
        return result < 0.0D ? result + period : result;
    }

    private double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }
}
