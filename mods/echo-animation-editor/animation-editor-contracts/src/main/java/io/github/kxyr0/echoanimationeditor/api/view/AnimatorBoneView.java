package io.github.kxyr0.echoanimationeditor.api.view;

import java.util.Objects;
import java.util.Optional;

public record AnimatorBoneView(String id, Optional<String> parentId, String displayName) {
    public AnimatorBoneView {
        id = requireText(id, "id");
        parentId = Objects.requireNonNull(parentId, "parentId");
        displayName = requireText(displayName, "displayName");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is blank");
        }
        return value;
    }
}
