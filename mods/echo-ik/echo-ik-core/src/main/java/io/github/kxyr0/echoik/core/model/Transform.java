package io.github.kxyr0.echoik.core.model;

import io.github.kxyr0.echoik.core.math.Quat;
import io.github.kxyr0.echoik.core.math.Vec3;
import java.util.Objects;

/**
 * Platform-neutral transform in solver space.
 */
public record Transform(Vec3 translation, Quat rotation, Vec3 scale) {
    public Transform {
        translation = Objects.requireNonNull(translation, "translation");
        rotation = Objects.requireNonNull(rotation, "rotation").normalized();
        scale = Objects.requireNonNull(scale, "scale");
    }

    public Transform withTranslation(Vec3 value) {
        return new Transform(value, rotation, scale);
    }

    public Transform withRotation(Quat value) {
        return new Transform(translation, value, scale);
    }

    public Transform blend(Transform target, double amount) {
        return new Transform(
                translation.lerp(target.translation, amount),
                rotation.slerp(target.rotation, amount),
                scale.lerp(target.scale, amount));
    }

    public boolean isFinite() {
        return translation.isFinite() && scale.isFinite();
    }

    public static Transform identity() {
        return new Transform(Vec3.zero(), Quat.identity(), new Vec3(1.0D, 1.0D, 1.0D));
    }

    public static Transform translated(Vec3 translation) {
        return identity().withTranslation(translation);
    }
}
