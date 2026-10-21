package io.github.kxyr0.echoplayerhudcontentadapter;

import io.github.kxyr0.echoplayer.forge.api.EchoPlayerService;
import io.github.kxyr0.echoplayer.forge.api.EchoPlayerServiceLookupEvent;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Looks up Echo Content Player only through its public Forge API.
 */
final class ContentPlayerServiceLookup {
    Optional<EchoPlayerService> lookup(MinecraftServer server) {
        EchoPlayerServiceLookupEvent event = new EchoPlayerServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }
}
