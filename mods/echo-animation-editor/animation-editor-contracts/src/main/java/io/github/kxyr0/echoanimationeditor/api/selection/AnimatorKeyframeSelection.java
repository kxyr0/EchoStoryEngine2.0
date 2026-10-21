package io.github.kxyr0.echoanimationeditor.api.selection;

import java.util.List;
import java.util.Objects;

public record AnimatorKeyframeSelection(List<String> keyframeIds) {
    public AnimatorKeyframeSelection {
        keyframeIds = List.copyOf(Objects.requireNonNull(keyframeIds, "keyframeIds"));
    }

    public boolean empty() {
        return keyframeIds.isEmpty();
    }
}
