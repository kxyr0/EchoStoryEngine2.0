package io.github.kxyr0.echoanimationeditor.api.view;

import java.util.Objects;

public record AnimatorClipView(String id, String displayName, int durationFrames, boolean readOnly) {
    public AnimatorClipView {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        if (durationFrames < 0) {
            throw new IllegalArgumentException("durationFrames is negative");
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
