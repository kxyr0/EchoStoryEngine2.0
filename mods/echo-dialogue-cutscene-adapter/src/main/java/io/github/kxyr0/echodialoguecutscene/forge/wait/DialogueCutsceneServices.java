package io.github.kxyr0.echodialoguecutscene.forge.wait;

import io.github.kxyr0.echocutscene.forge.api.event.CutsceneServiceLookupEvent;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class DialogueCutsceneServices {
    public Optional<DialogueService> dialogue(MinecraftServer server) {
        EchoDialogueServiceLookupEvent event = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }

    public Optional<CutsceneService> cutscene(MinecraftServer server) {
        CutsceneServiceLookupEvent event = new CutsceneServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }
}
