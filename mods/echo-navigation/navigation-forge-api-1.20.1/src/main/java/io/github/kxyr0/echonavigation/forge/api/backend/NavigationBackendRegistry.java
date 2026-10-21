package io.github.kxyr0.echonavigation.forge.api.backend;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import java.util.List;
import java.util.Optional;

public interface NavigationBackendRegistry {
    RegistryResult register(NavigationBackend backend);

    Optional<NavigationBackend> backend(NavigationBackendId id);

    List<NavigationBackendId> ids();

    enum RegistryResult {
        REGISTERED,
        DUPLICATE_ID
    }
}
