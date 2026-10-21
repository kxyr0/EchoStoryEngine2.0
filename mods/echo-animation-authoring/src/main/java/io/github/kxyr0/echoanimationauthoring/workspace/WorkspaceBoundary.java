package io.github.kxyr0.echoanimationauthoring.workspace;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates authoring workspace reads and writes.
 */
public final class WorkspaceBoundary {
    public Path requireInside(Optional<Path> configuredWorkspace, Path candidate) {
        Objects.requireNonNull(configuredWorkspace, "configuredWorkspace");
        Objects.requireNonNull(candidate, "candidate");
        Path workspace = configuredWorkspace.orElseThrow(() ->
                new IllegalStateException("Workspace не настроен; запись запрещена."));
        Path normalizedWorkspace = workspace.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedWorkspace)) {
            throw new IllegalArgumentException("Путь выходит за пределы workspace: " + normalizedCandidate);
        }
        return normalizedCandidate;
    }
}

