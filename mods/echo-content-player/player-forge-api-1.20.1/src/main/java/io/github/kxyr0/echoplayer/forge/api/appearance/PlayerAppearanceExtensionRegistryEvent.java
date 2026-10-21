package io.github.kxyr0.echoplayer.forge.api.appearance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Synchronous lookup event for neutral appearance extension registration.
 */
public final class PlayerAppearanceExtensionRegistryEvent extends Event {
    private final List<PlayerAppearanceExtensionRegistry> registries = new ArrayList<>();

    public void provide(PlayerAppearanceExtensionRegistry registry) {
        registries.add(Objects.requireNonNull(registry, "registry"));
    }

    public List<PlayerAppearanceExtensionRegistry> registries() {
        return List.copyOf(registries);
    }
}
