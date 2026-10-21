package io.github.kxyr0.echowardrobe.core.migration;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Legacy .es file found under a texture asset tree.
 */
public record LegacyWardrobeScript(
        Path originalPath,
        Path canonicalPath,
        boolean emptyPlaceholder,
        String diagnosticCode
) {
    public LegacyWardrobeScript {
        originalPath = Objects.requireNonNull(originalPath, "originalPath");
        canonicalPath = Objects.requireNonNull(canonicalPath, "canonicalPath");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnosticCode");
    }
}
