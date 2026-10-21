package io.github.kxyr0.echocutscene.api.camera;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Objects;

public record CameraPosition(Vector3 value) {
    public CameraPosition {
        value = Objects.requireNonNull(value, "value");
    }

    public static CameraPosition of(double x, double y, double z) {
        return new CameraPosition(new Vector3(x, y, z));
    }
}
