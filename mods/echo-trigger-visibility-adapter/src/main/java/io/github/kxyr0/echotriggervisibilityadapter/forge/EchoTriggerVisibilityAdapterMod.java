package io.github.kxyr0.echotriggervisibilityadapter.forge;

import io.github.kxyr0.echovisibility.forge.api.lifecycle.VisibilityLifecycleEvent;
import io.github.kxyr0.echotriggervisibilityadapter.forge.command.VisibilityInspectCommand;
import io.github.kxyr0.echotriggervisibilityadapter.forge.story.VisibilityStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoTriggerVisibilityAdapterMod.MOD_ID)
public final class EchoTriggerVisibilityAdapterMod {
    public static final String MOD_ID = "echo_trigger_visibility_adapter";
    private final VisibilityEventRouter router = new VisibilityEventRouter();
    private final VisibilityStoryExtensionFactory storyActions = new VisibilityStoryExtensionFactory();
    private MinecraftServer server;

    public EchoTriggerVisibilityAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::stopped);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::visibility);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::storyExtensions);
    }

    private void started(ServerStartedEvent event) {
        server = event.getServer();
    }

    private void stopped(ServerStoppedEvent event) {
        if (server == event.getServer()) {
            storyActions.close(server);
            server = null;
        }
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (server == event.getServer() && event.phase == TickEvent.Phase.END) {
            router.tick(server);
            storyActions.tick(server);
        }
    }

    private void visibility(VisibilityLifecycleEvent event) {
        if (server == event.player().server) {
            router.accept(event);
        }
    }

    private void commands(RegisterCommandsEvent event) {
        VisibilityInspectCommand.register(event.getDispatcher());
    }

    private void storyExtensions(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(storyActions);
    }
}
