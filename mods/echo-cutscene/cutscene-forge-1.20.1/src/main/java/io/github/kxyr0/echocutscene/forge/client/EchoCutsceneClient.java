package io.github.kxyr0.echocutscene.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kxyr0.echocutscene.forge.client.cinematic.ClientCinematicMode;
import io.github.kxyr0.echocutscene.forge.client.fade.ClientCutsceneFadeOverlay;
import io.github.kxyr0.echocutscene.forge.client.fade.ClientScreenBlackOverlay;
import io.github.kxyr0.echocutscene.forge.network.ClientboundCutsceneStartPacket;
import io.github.kxyr0.echocutscene.forge.network.ClientboundScreenBlackPacket;
import io.github.kxyr0.echocutscene.forge.network.EchoCutsceneNetwork;
import io.github.kxyr0.echocutscene.forge.network.ServerboundCutsceneSkipPacket;
import io.github.kxyr0.echocutscene.forge.network.authoring.ClientboundAuthoringModePacket;
import io.github.kxyr0.echocutscene.forge.network.authoring.ClientboundAuthoringStartPacket;
import io.github.kxyr0.echocutscene.forge.network.authoring.ClientboundAuthoringTargetPacket;
import java.util.OptionalDouble;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public final class EchoCutsceneClient {
    private static final ClientCameraPlayback PLAYBACK = new ClientCameraPlayback();
    private static final ClientScreenBlackOverlay SCREEN_BLACK = new ClientScreenBlackOverlay();
    private static final ClientAuthoringSession AUTHORING = new ClientAuthoringSession();
    static final KeyMapping CUTSCENE_RECORD = new KeyMapping("key.echo_debug.cutscene_record",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F4, "key.categories.echo_debug");
    static final KeyMapping CUTSCENE_PITCH_FLIGHT = new KeyMapping("key.echo_debug.cutscene_pitch_flight",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, "key.categories.echo_debug");
    static final KeyMapping CUTSCENE_SET_TARGET = new KeyMapping("key.echo_debug.cutscene_set_target",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, "key.categories.echo_debug");
    static final KeyMapping CUTSCENE_CAMERA_MODE = new KeyMapping("key.echo_debug.cutscene_camera_mode",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.echo_debug");

    private EchoCutsceneClient() {
    }

    public static void bootstrap(IEventBus modBus) {
        modBus.addListener(EchoCutsceneClient::onRegisterKeys);
        MinecraftForge.EVENT_BUS.register(new ClientInputSuppressor(PLAYBACK, AUTHORING));
        MinecraftForge.EVENT_BUS.register(new ClientAuthoringHudOverlay(AUTHORING));
        MinecraftForge.EVENT_BUS.register(new ClientCinematicMode(PLAYBACK));
        MinecraftForge.EVENT_BUS.register(new ClientCutsceneFadeOverlay(PLAYBACK));
        MinecraftForge.EVENT_BUS.register(SCREEN_BLACK);
        MinecraftForge.EVENT_BUS.addListener(EchoCutsceneClient::onComputeFov);
        MinecraftForge.EVENT_BUS.addListener(EchoCutsceneClient::onClientLoggingOut);
    }

    public static void begin(ClientboundCutsceneStartPacket packet) {
        long startedAtMillis = Util.getMillis() - Math.round(packet.elapsedMillis() / packet.speed());
        PLAYBACK.begin(packet.playbackId(), packet.track(), startedAtMillis, packet.speed(),
                packet.endBehavior());
        PLAYBACK.hudPolicy(packet.hudPolicy());
        EchoCutsceneNetwork.ready(packet.playbackId());
    }

    public static void setBlack(ClientboundScreenBlackPacket packet) {
        SCREEN_BLACK.set(packet.canvasGroup(), packet.timeMillis());
    }

    public static boolean hudSuppressed() {
        return PLAYBACK.hudSuppressed() || SCREEN_BLACK.active();
    }

    public static void end(io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId playbackId) {
        PLAYBACK.end(playbackId);
    }

    public static void beginAuthoring(ClientboundAuthoringStartPacket packet) {
        AUTHORING.begin(packet.sessionId(), packet.cutsceneId(), packet.initialPose(), packet.cameraSpeed());
    }

    public static void endAuthoring(String sessionId) {
        AUTHORING.end(sessionId);
    }

    public static void setAuthoringMode(ClientboundAuthoringModePacket packet) {
        AUTHORING.setCameraMode(packet.sessionId(), packet.mode());
    }

    public static void setAuthoringTarget(ClientboundAuthoringTargetPacket packet) {
        packet.target().ifPresentOrElse(target -> AUTHORING.setTarget(packet.sessionId(), target),
                () -> AUTHORING.clearTarget(packet.sessionId()));
    }

    public static void requestSkip(String requestId) {
        if (PLAYBACK.playing()) {
            EchoCutsceneNetwork.sendToServer(new ServerboundCutsceneSkipPacket(requestId));
        }
    }

    private static void onComputeFov(ViewportEvent.ComputeFov event) {
        OptionalDouble fov = PLAYBACK.currentFov();
        if (fov.isEmpty()) {
            fov = AUTHORING.currentFov();
        }
        if (fov.isPresent()) {
            event.setFOV(fov.orElseThrow());
        }
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PLAYBACK.clear();
        SCREEN_BLACK.clear();
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(CUTSCENE_RECORD);
        event.register(CUTSCENE_PITCH_FLIGHT);
        event.register(CUTSCENE_SET_TARGET);
        event.register(CUTSCENE_CAMERA_MODE);
    }
}
