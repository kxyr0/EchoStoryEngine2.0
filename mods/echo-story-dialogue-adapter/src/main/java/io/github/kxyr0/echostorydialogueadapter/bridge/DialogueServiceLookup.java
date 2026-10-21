package io.github.kxyr0.echostorydialogueadapter.bridge;

import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Synchronous server-scoped lookup for the public Echo Dialogue service.
 */
final class DialogueServiceLookup {
    DialogueService lookup(MinecraftServer server) {
        EchoDialogueServiceLookupEvent event = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        List<DialogueService> providers = event.providers();
        if (providers.size() != 1) {
            throw new IllegalStateException("Echo Dialogue service providers: " + providers.size() + ".");
        }
        return providers.get(0);
    }
}
