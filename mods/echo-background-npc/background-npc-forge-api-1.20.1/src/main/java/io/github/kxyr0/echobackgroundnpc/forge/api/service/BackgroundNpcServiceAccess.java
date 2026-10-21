package io.github.kxyr0.echobackgroundnpc.forge.api.service;

import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class BackgroundNpcServiceAccess {
    private BackgroundNpcServiceAccess() { }
    public static Optional<BackgroundNpcService> find(MinecraftServer server) {
        BackgroundNpcServiceLookupEvent event = new BackgroundNpcServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().size() == 1 ? Optional.of(event.providers().get(0)) : Optional.empty();
    }
}
