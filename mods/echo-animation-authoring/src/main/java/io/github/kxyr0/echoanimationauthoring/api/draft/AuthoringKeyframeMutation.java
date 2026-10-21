package io.github.kxyr0.echoanimationauthoring.api.draft;

import java.util.Objects;

public record AuthoringKeyframeMutation(
        String boneId,
        int frame,
        AuthoringTransform transform,
        String interpolation
) {
    public AuthoringKeyframeMutation {
        boneId = requireText(boneId, "boneId");
        transform = Objects.requireNonNull(transform, "transform");
        interpolation = requireText(interpolation, "interpolation");
        if (frame < 0) {
            throw new IllegalArgumentException("frame is negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is blank");
        }
        return value;
    }
}
