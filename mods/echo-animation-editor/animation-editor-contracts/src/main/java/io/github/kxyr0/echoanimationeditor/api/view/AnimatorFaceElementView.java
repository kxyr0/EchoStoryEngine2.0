package io.github.kxyr0.echoanimationeditor.api.view;

import java.util.Objects;

public record AnimatorFaceElementView(String id, String displayName, String layerId) {
    public AnimatorFaceElementView {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        layerId = requireText(layerId, "layerId");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is blank");
        }
        return value;
    }
}
