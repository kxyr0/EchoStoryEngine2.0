package io.github.kxyr0.echotriggertimeadapter.forge;

import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceBridge;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceBridge;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceLookupEvent;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class TimeAdapterServices {
    private TimeAdapterServices() { }

    public static Optional<EchoTimeServiceBridge> time(MinecraftServer server) {
        return EchoTimeServiceAccess.find(server);
    }

    public static Optional<TriggerServiceBridge> trigger(MinecraftServer server) {
        TriggerServiceLookupEvent event = new TriggerServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.bridge();
    }

    public static Optional<DialogueService> dialogue(MinecraftServer server) {
        EchoDialogueServiceLookupEvent event = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }
}
