package io.github.kxyr0.echoik.core.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable current pose in solver space. Solvers return a new Pose instead of mutating input animation.
 */
public final class Pose {
    private final Map<String, Transform> transforms;

    public Pose(Map<String, Transform> transforms) {
        Objects.requireNonNull(transforms, "transforms");
        Map<String, Transform> values = new LinkedHashMap<>();
        transforms.forEach((id, transform) -> values.put(requireId(id), Objects.requireNonNull(transform, id)));
        this.transforms = Map.copyOf(values);
    }

    public Optional<Transform> transform(String boneId) {
        return Optional.ofNullable(transforms.get(boneId));
    }

    public Transform requireTransform(String boneId) {
        return transform(boneId).orElseThrow(() -> new IllegalArgumentException("Pose has no bone: " + boneId));
    }

    public Pose withTransform(String boneId, Transform transform) {
        Map<String, Transform> values = new LinkedHashMap<>(transforms);
        values.put(requireId(boneId), Objects.requireNonNull(transform, "transform"));
        return new Pose(values);
    }

    public Pose withTransforms(Map<String, Transform> updates) {
        Map<String, Transform> values = new LinkedHashMap<>(transforms);
        updates.forEach((id, transform) -> values.put(requireId(id), Objects.requireNonNull(transform, id)));
        return new Pose(values);
    }

    public Map<String, Transform> transforms() {
        return transforms;
    }

    private static String requireId(String value) {
        String checked = Objects.requireNonNull(value, "boneId").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("boneId must not be blank.");
        }
        return checked;
    }
}
