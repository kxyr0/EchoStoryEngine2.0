package io.github.kxyr0.echonavigation.api.backend;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import java.util.List;
import java.util.Objects;

public record NavigationBackendDescriptor(
        NavigationBackendId id,
        String displayName,
        int schemaVersion,
        boolean available,
        List<String> capabilities) {
    public NavigationBackendDescriptor {
        id = Objects.requireNonNull(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty() || schemaVersion < 1) {
            throw new IllegalArgumentException("Backend descriptor is invalid.");
        }
        capabilities = List.copyOf(Objects.requireNonNull(capabilities, "capabilities"));
    }
}
