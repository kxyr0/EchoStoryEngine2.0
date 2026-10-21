package io.github.kxyr0.echosavemigration.compile;

import java.util.Objects;

public record MigrationSourceInput(String contentNamespace, String sourcePath, String sourceText, long revision) {
    public MigrationSourceInput {
        contentNamespace = Objects.requireNonNull(contentNamespace, "contentNamespace");
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        sourceText = Objects.requireNonNull(sourceText, "sourceText");
        if (!contentNamespace.matches("[a-z0-9_.-]+") || revision < 1) {
            throw new IllegalArgumentException("Invalid migration source metadata.");
        }
    }
}
