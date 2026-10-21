package io.github.kxyr0.echowardrobe.forge.api;

import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;

/**
 * Public explicit Forge bridge for resolving Echo Wardrobe service.
 */
public final class EchoWardrobeServiceBridge {
    private EchoWardrobeServiceBridge() {
    }

    public static Optional<EchoWardrobeService> lookup() {
        EchoWardrobeServiceLookupEvent event = new EchoWardrobeServiceLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().size() != 1) {
            return Optional.empty();
        }
        return Optional.of(event.providers().get(0));
    }
}
