package io.github.kxyr0.echotime.forge.api;

import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class EchoTimeServiceAccess {
    private EchoTimeServiceAccess() { }

    public static Optional<EchoTimeServiceBridge> find(MinecraftServer server) {
        EchoTimeServiceLookupEvent event = new EchoTimeServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.bridge();
    }
}
