package io.github.kxyr0.echoteleportmagic.forge.integration;

import io.github.kxyr0.echoaudio.forge.server.AudioLocationLookupEvent;
import io.github.kxyr0.echoteleportmagic.api.TeleportDestination;
import net.minecraftforge.common.MinecraftForge;

/** Adapts the existing shared Location lookup, without a parallel registry. */
public final class NamedTeleportDestination {
    private NamedTeleportDestination() { }
    public static TeleportDestination resolve(String reference) {
        var event = new AudioLocationLookupEvent(reference);
        MinecraftForge.EVENT_BUS.post(event);
        var position = event.require();
        return new TeleportDestination(reference, position.dimension(), position.x(), position.y(), position.z(), 0, 0);
    }
}
