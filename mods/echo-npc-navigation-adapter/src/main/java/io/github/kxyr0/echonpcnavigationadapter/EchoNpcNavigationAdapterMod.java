package io.github.kxyr0.echonpcnavigationadapter;

import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolverRegistrationEvent;
import io.github.kxyr0.echonpcnavigationadapter.dialogue.DialogueNavigationWaits;
import io.github.kxyr0.echonpcnavigationadapter.dialogue.NpcDialogueNavigationListener;
import io.github.kxyr0.echonpcnavigationadapter.navigation.NpcNavigationAgentResolver;
import io.github.kxyr0.echonpcnavigationadapter.runtime.AdapterSessions;
import io.github.kxyr0.echonpcnavigationadapter.story.NpcNavigationStoryExtensionFactory;
import io.github.kxyr0.echonpcnavigationadapter.trigger.NpcTriggerNavigationListener;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoNpcNavigationAdapterMod.MOD_ID)
public final class EchoNpcNavigationAdapterMod {
    public static final String MOD_ID = "echo_npc_navigation_adapter";

    private final AdapterSessions sessions = new AdapterSessions();
    private final DialogueNavigationWaits dialogueWaits = new DialogueNavigationWaits();

    public EchoNpcNavigationAdapterMod() {
        MinecraftForge.EVENT_BUS.register(new NpcDialogueNavigationListener(dialogueWaits));
        MinecraftForge.EVENT_BUS.register(new NpcTriggerNavigationListener());
        MinecraftForge.EVENT_BUS.addListener(this::registerNavigationResolver);
        MinecraftForge.EVENT_BUS.addListener(this::registerStoryExtension);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
    }

    private void registerNavigationResolver(NavigationAgentResolverRegistrationEvent event) {
        event.registry().register(new NpcNavigationAgentResolver());
    }

    private void registerStoryExtension(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new NpcNavigationStoryExtensionFactory(sessions));
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            sessions.tick(event.getServer());
            dialogueWaits.tick(event.getServer());
        }
    }

    private void serverStopping(ServerStoppingEvent event) {
        sessions.close(event.getServer());
        dialogueWaits.close(event.getServer());
    }
}
