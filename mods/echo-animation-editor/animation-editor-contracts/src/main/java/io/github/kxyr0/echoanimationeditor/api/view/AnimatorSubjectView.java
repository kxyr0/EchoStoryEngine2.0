package io.github.kxyr0.echoanimationeditor.api.view;

import java.util.Objects;

public record AnimatorSubjectView(String id, String displayName, boolean materialized) {
    public AnimatorSubjectView {
        id = requireText(id, "id");
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
