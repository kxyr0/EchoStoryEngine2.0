package io.github.kxyr0.echoik.core.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable bone definition. Humanoid bones are conventions, not a core requirement.
 */
public record Bone(String id, Optional<String> parentId, Transform restTransform, double length) {
    public Bone {
        id = requireId(id, "id");
        parentId = Objects.requireNonNull(parentId, "parentId").map(value -> requireId(value, "parentId"));
        restTransform = Objects.requireNonNull(restTransform, "restTransform");
        if (!Double.isFinite(length) || length < 0.0D) {
            throw new IllegalArgumentException("length must be finite and non-negative.");
        }
    }

    public Bone(String id, String parentId, Transform restTransform, double length) {
        this(id, Optional.ofNullable(parentId), restTransform, length);
    }

    private static String requireId(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
