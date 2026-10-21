package io.github.kxyr0.echostorynpcadapter;

import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import io.github.kxyr0.echostorynpcadapter.story.AdapterSessions;
import io.github.kxyr0.echostorynpcadapter.story.StoryNpcExtensionFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoStoryNpcAdapterMod.MOD_ID)
public final class EchoStoryNpcAdapterMod {
    public static final String MOD_ID = "echo_story_npc_adapter";

    private final AdapterSessions sessions = new AdapterSessions();

    public EchoStoryNpcAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerExtension);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
    }

    private void registerExtension(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new StoryNpcExtensionFactory(sessions));
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            sessions.tick(event.getServer());
        }
    }

    private void serverStopping(ServerStoppingEvent event) {
        sessions.close(event.getServer());
    }
}
