package io.github.kxyr0.echoplayerfollow.client;

import io.github.kxyr0.echoplayerfollow.network.ClientboundFollowPromptPacket;
import io.github.kxyr0.echoplayerfollow.network.ClientboundFollowStartPacket;
import io.github.kxyr0.echoplayerfollow.network.ClientboundFollowStopPacket;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public final class EchoPlayerFollowClient {
    private static final ClientFollowState STATE = new ClientFollowState();
    private static final FollowMovementController MOVEMENT = new FollowMovementController();
    private static final ClientFollowRuntime RUNTIME = new ClientFollowRuntime(STATE, MOVEMENT);
    private static final FollowHudOverlay HUD = new FollowHudOverlay(STATE);

    private EchoPlayerFollowClient() {
    }

    public static void bootstrap(IEventBus modBus) {
        modBus.addListener(FollowKeyMappings::register);
        modBus.addListener(EchoPlayerFollowClient::registerOverlays);
        MinecraftForge.EVENT_BUS.addListener(RUNTIME::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(RUNTIME::onMovementInput);
        MinecraftForge.EVENT_BUS.addListener(RUNTIME::onLoggingOut);
    }

    public static void prompt(ClientboundFollowPromptPacket packet) {
        STATE.prompt(packet, net.minecraft.Util.getMillis());
    }

    public static void start(ClientboundFollowStartPacket packet) {
        STATE.start(packet);
    }

    public static void stop(ClientboundFollowStopPacket packet) {
        STATE.stop();
        MOVEMENT.release();
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("echo_player_follow", HUD::render);
    }
}
