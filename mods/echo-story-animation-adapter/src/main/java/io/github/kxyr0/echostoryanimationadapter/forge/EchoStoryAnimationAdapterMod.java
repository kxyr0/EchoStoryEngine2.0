package io.github.kxyr0.echostoryanimationadapter.forge;

import io.github.kxyr0.echostoryanimationadapter.bridge.AnimationBridgeExtensionFactory;
import io.github.kxyr0.echostoryanimationadapter.runtime.AdapterSessions;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Thin Forge entrypoint for the Story Animation adapter.
 */
@Mod(EchoStoryAnimationAdapterMod.MOD_ID)
public final class EchoStoryAnimationAdapterMod {
    public static final String MOD_ID = "echo_story_animation_adapter";

    private final AdapterSessions sessions = new AdapterSessions();

    public EchoStoryAnimationAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerStoryExtension);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
    }

    private void registerStoryExtension(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new AnimationBridgeExtensionFactory(sessions));
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
