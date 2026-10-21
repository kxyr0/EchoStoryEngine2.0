package io.github.kxyr0.echonpcinteraction.forge.api.provider;

import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionProviderId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class NpcInteractionProviderRegistry {
    private final Map<NpcInteractionProviderId, NpcInteractionProvider> providers = new LinkedHashMap<>();

    public void register(NpcInteractionProvider provider) {
        NpcInteractionProvider checked = Objects.requireNonNull(provider, "provider");
        providers.putIfAbsent(checked.id(), checked);
    }

    public Optional<NpcInteractionProvider> find(NpcInteractionProviderId providerId) {
        return Optional.ofNullable(providers.get(providerId));
    }

    public List<NpcInteractionProvider> providers() {
        return List.copyOf(providers.values());
    }
}
