package io.github.kxyr0.echoscene.compile.source;

import java.util.Objects;

public record SceneSourceInput(String contentNamespace, String sourcePath, String sourceText, long revision) {
    public SceneSourceInput {
        contentNamespace = Objects.requireNonNull(contentNamespace, "contentNamespace").trim();
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        sourceText = Objects.requireNonNull(sourceText, "sourceText");
        if (!contentNamespace.matches("[a-z0-9_.-]+") || revision < 1) {
            throw new IllegalArgumentException("Invalid scene source metadata.");
        }
    }
}
