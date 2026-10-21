package io.github.kxyr0.echoevents.forge.api;

import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.core.runtime.EventDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class EchoEvents {
    private EchoEvents() { }
    public static EventDispatcher service(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Echo events require server thread");
        var lookup = new EchoEventsLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.service().orElseThrow(() -> new IllegalStateException("Echo Events is unavailable"));
    }
    public static boolean publish(MinecraftServer server, EventEnvelope<?> event, EventMutationPolicy policy) {
        return service(server).publish(event, policy);
    }
}
