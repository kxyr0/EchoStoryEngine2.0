package io.github.kxyr0.echoeffects.forge.client;

import io.github.kxyr0.echoeffects.core.contribution.EffectContributionSnapshot;
import io.github.kxyr0.echoeffects.forge.ForgeEffectServices;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public final class EchoEffectsClientBootstrap {
    private static final ForgeEffectClientState STATE = new ForgeEffectClientState();
    private static final ForgeScreenOverlay OVERLAY = new ForgeScreenOverlay(STATE);
    private static final ForgeCameraEffects CAMERA = new ForgeCameraEffects(STATE);

    private EchoEffectsClientBootstrap() {
    }

    public static void bootstrap(IEventBus modBus, ForgeEffectServices services) {
        STATE.update(EffectContributionSnapshot.neutral());
        modBus.addListener(EchoEffectsClientBootstrap::onRegisterOverlay);
        MinecraftForge.EVENT_BUS.addListener(EchoEffectsClientBootstrap::onComputeFov);
    }

    private static void onRegisterOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("echo_effects", OVERLAY::render);
    }

    private static void onComputeFov(ViewportEvent.ComputeFov event) {
        CAMERA.onComputeFov(event);
    }
}
