package io.github.kxyr0.echodebug.core.path;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import java.nio.file.Path;
import java.util.Objects;

public record TriggerTargetPath(Path root, Path finalPath) {
    public TriggerTargetPath {
        root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        finalPath = Objects.requireNonNull(finalPath, "finalPath").toAbsolutePath().normalize();
        if (!finalPath.startsWith(root)) {
            throw new IllegalArgumentException("Target path must stay under trigger root.");
        }
    }

    public static DebugOperationResult<TriggerTargetPath> plan(Path gameDirectory, TriggerIdPlan id) {
        try {
            Path root = gameDirectory.toAbsolutePath().normalize().resolve("story").resolve("triggers");
            Path target = root.resolve(id.namespace()).resolve(id.path() + ".trigger.es");
            return DebugOperationResult.success(new TriggerTargetPath(root, target));
        } catch (RuntimeException exception) {
            return DebugOperationResult.failure(DebugDiagnosticCode.DEBUG_TRIGGER_PATH_INVALID,
                    exception.getMessage(), null);
        }
    }
}
