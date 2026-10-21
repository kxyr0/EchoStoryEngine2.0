package io.github.kxyr0.echoplayeractor.core.rotation;

import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInterpolation;
import java.util.Objects;

public final class PlayerRotationProgress {
    public PlayerActorRotationSample sample(
            double startYaw,
            double startPitch,
            double targetYaw,
            double targetPitch,
            int totalTicks,
            int elapsedTicks,
            PlayerActorInterpolation interpolation
    ) {
        Objects.requireNonNull(interpolation, "interpolation");
        if (totalTicks <= 0 || elapsedTicks >= totalTicks) {
            return new PlayerActorRotationSample(AngleMath.wrapDegrees(targetYaw),
                    AngleMath.clampPitch(targetPitch), true);
        }
        double clampedTick = Math.max(0, elapsedTicks);
        double ratio = ease(clampedTick / totalTicks, interpolation);
        double yaw = startYaw + AngleMath.shortestDelta(startYaw, targetYaw) * ratio;
        double pitch = startPitch + (AngleMath.clampPitch(targetPitch) - startPitch) * ratio;
        return new PlayerActorRotationSample(AngleMath.wrapDegrees(yaw), AngleMath.clampPitch(pitch), false);
    }

    private double ease(double value, PlayerActorInterpolation interpolation) {
        double t = Math.max(0.0D, Math.min(1.0D, value));
        return switch (interpolation) {
            case LINEAR -> t;
            case SMOOTH -> t * t * (3.0D - 2.0D * t);
            case CUBIC -> t * t * t;
        };
    }
}
