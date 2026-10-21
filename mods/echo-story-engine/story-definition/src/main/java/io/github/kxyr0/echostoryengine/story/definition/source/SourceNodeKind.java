package io.github.kxyr0.echostoryengine.story.definition.source;

import java.util.Objects;
import java.util.Optional;

/**
 * Supported source node kinds for the M2 source format.
 */
public enum SourceNodeKind {
    START("start"),
    SET_VARIABLE("set_variable"),
    CONDITION("condition"),
    EXTERNAL_ACTION("external_action"),
    FINISH("finish");

    private final String jsonName;

    SourceNodeKind(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }

    public static Optional<SourceNodeKind> from(String raw) {
        String checked = Objects.requireNonNull(raw, "raw").trim();
        return switch (checked) {
            case "start" -> Optional.of(START);
            case "set_variable" -> Optional.of(SET_VARIABLE);
            case "condition" -> Optional.of(CONDITION);
            case "external_action" -> Optional.of(EXTERNAL_ACTION);
            case "finish" -> Optional.of(FINISH);
            default -> Optional.empty();
        };
    }
}
