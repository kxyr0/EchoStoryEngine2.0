package io.github.kxyr0.echodevscenarios.compile.source;

import java.util.Objects;

public record ScenarioSourceInput(String sourcePath, String sourceText) {
    public ScenarioSourceInput {
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        sourceText = Objects.requireNonNull(sourceText, "sourceText");
    }
}
