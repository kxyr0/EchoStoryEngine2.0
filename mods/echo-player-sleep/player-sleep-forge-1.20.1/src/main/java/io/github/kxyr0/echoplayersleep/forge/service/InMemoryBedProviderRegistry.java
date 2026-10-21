package io.github.kxyr0.echoplayersleep.forge.service;

import io.github.kxyr0.echoplayersleep.forge.api.bed.PlayerSleepBedProvider;
import io.github.kxyr0.echoplayersleep.forge.api.bed.PlayerSleepBedProviderRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class InMemoryBedProviderRegistry implements PlayerSleepBedProviderRegistry {
    private final Map<String, PlayerSleepBedProvider> providers = new LinkedHashMap<>();

    @Override
    public boolean register(PlayerSleepBedProvider provider) {
        Objects.requireNonNull(provider, "provider");
        if (providers.containsKey(provider.providerId())) {
            return false;
        }
        providers.put(provider.providerId(), provider);
        return true;
    }

    @Override
    public Optional<PlayerSleepBedProvider> providerFor(Level level, BlockPos pos) {
        return providers.values().stream().filter(provider -> provider.supports(level, pos)).findFirst();
    }

    @Override
    public List<PlayerSleepBedProvider> providers() {
        return List.copyOf(providers.values());
    }
}
