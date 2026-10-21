package io.github.kxyr0.echonavigation.forge.api.backend;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;

public record NavigationBackendView(
        NavigationBackendId id,
        String displayName,
        int schemaVersion,
        boolean available) {
}
