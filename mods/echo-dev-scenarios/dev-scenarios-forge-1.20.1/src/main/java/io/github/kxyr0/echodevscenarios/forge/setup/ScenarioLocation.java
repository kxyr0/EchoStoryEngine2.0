package io.github.kxyr0.echodevscenarios.forge.setup;

import java.util.Objects;

public record ScenarioLocation(String id, String dimension, double x, double y, double z, float yaw, float pitch) {
    public ScenarioLocation {
        id = require(id, "id");
        dimension = require(dimension, "dimension");
        if (!dimension.contains(":")) throw new IllegalArgumentException("Location dimension must be namespaced.");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Location coordinates must be finite.");
        }
    }

    private static String require(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("Location " + name + " cannot be blank.");
        return checked;
    }
}
