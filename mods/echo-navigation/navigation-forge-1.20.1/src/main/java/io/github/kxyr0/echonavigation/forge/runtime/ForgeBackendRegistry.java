package io.github.kxyr0.echonavigation.forge.runtime;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.forge.api.backend.NavigationBackend;
import io.github.kxyr0.echonavigation.forge.api.backend.NavigationBackendRegistry;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ForgeBackendRegistry implements NavigationBackendRegistry {
    private final Map<NavigationBackendId, NavigationBackend> backends = new LinkedHashMap<>();

    @Override
    public RegistryResult register(NavigationBackend backend) {
        if (backends.containsKey(backend.id())) {
            return RegistryResult.DUPLICATE_ID;
        }
        backends.put(backend.id(), backend);
        return RegistryResult.REGISTERED;
    }

    @Override
    public Optional<NavigationBackend> backend(NavigationBackendId id) {
        return Optional.ofNullable(backends.get(id));
    }

    @Override
    public List<NavigationBackendId> ids() {
        return backends.keySet().stream().sorted(Comparator.naturalOrder()).toList();
    }
}
