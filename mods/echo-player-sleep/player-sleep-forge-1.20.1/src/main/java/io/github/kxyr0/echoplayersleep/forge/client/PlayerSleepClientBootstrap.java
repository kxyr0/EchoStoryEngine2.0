package io.github.kxyr0.echoplayersleep.forge.client;

import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public final class PlayerSleepClientBootstrap {
    private static final ClientSleepTransitionMirror MIRROR = new ClientSleepTransitionMirror();
    private static final PlayerSleepOverlayRenderer RENDERER = new PlayerSleepOverlayRenderer(MIRROR);
    private static final PlayerSleepHudHider HUD_HIDER = new PlayerSleepHudHider(MIRROR);

    private PlayerSleepClientBootstrap() {
    }

    public static void bootstrap(IEventBus modBus) {
        modBus.addListener((RegisterGuiOverlaysEvent event) ->
                event.registerAboveAll("echo_player_sleep_eyes", RENDERER::render));
        MinecraftForge.EVENT_BUS.addListener(HUD_HIDER::onRenderGuiOverlayPre);
        MinecraftForge.EVENT_BUS.addListener(MIRROR::onTransition);
        MinecraftForge.EVENT_BUS.addListener(PlayerSleepChatHider::onClientTick);
    }
}
