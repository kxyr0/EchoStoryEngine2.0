package io.github.kxyr0.echoquests.forge.api;

import io.github.kxyr0.echoquests.api.runtime.QuestService;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class EchoQuestServiceAccess {
    private EchoQuestServiceAccess() { }

    public static Optional<QuestService> find(MinecraftServer server) {
        QuestServiceLookupEvent event = new QuestServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.service();
    }
}
