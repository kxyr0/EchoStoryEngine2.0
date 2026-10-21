package io.github.kxyr0.echoplayer.forge.api.appearance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraftforge.common.MinecraftForge;

/**
 * Public explicit Forge bridge for resolving appearance extension registry.
 */
public final class PlayerAppearanceExtensionRegistryBridge {
    private static final List<PlayerAppearanceContribution> PENDING = new ArrayList<>();
    private static final AtomicReference<PlayerAppearanceExtensionRegistry> ATTACHED = new AtomicReference<>();

    private PlayerAppearanceExtensionRegistryBridge() {
    }

    public static synchronized void attach(PlayerAppearanceExtensionRegistry registry) {
        PlayerAppearanceExtensionRegistry checked = Objects.requireNonNull(registry, "registry");
        ATTACHED.set(checked);
        PENDING.forEach(checked::register);
        PENDING.clear();
    }

    public static synchronized void register(PlayerAppearanceContribution contribution) {
        PlayerAppearanceContribution checked = Objects.requireNonNull(contribution, "contribution");
        PlayerAppearanceExtensionRegistry registry = ATTACHED.get();
        if (registry == null) {
            PENDING.add(checked);
        } else {
            registry.register(checked);
        }
    }

    public static Optional<PlayerAppearanceExtensionRegistry> lookup() {
        synchronized (PlayerAppearanceExtensionRegistryBridge.class) {
            PlayerAppearanceExtensionRegistry registry = ATTACHED.get();
            if (registry != null) {
                return Optional.of(registry);
            }
        }
        PlayerAppearanceExtensionRegistryEvent event = new PlayerAppearanceExtensionRegistryEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.registries().size() != 1) {
            return Optional.empty();
        }
        return Optional.of(event.registries().get(0));
    }
}
