package io.github.kxyr0.echotime.script.dialogue;

import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

final class TimeDialogueServices {
    private TimeDialogueServices() { }

    static Optional<DialogueService> find(MinecraftServer server) {
        EchoDialogueServiceLookupEvent event = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().size() == 1 ? Optional.of(event.providers().get(0)) : Optional.empty();
    }
}
