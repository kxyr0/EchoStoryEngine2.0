package io.github.kxyr0.echoitem.core.manifest;

import io.github.kxyr0.echoitem.api.EchoItemId;
import java.util.List;
import java.util.Objects;

/**
 * Startup or live registry planning result.
 */
public record ItemRegistrationPlan(
        List<EchoItemId> registeredIds,
        List<PendingItemRegistration> pendingRestartIds,
        List<EchoItemId> missingDefinitionPlaceholders,
        List<EchoItemId> registryConflicts
) {
    public ItemRegistrationPlan {
        registeredIds = List.copyOf(Objects.requireNonNull(registeredIds, "registeredIds"));
        pendingRestartIds = List.copyOf(Objects.requireNonNull(pendingRestartIds, "pendingRestartIds"));
        missingDefinitionPlaceholders = List.copyOf(Objects.requireNonNull(
                missingDefinitionPlaceholders, "missingDefinitionPlaceholders"));
        registryConflicts = List.copyOf(Objects.requireNonNull(registryConflicts, "registryConflicts"));
    }
}
