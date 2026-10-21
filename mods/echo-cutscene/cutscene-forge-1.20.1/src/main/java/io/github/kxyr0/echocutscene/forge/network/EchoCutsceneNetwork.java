package io.github.kxyr0.echocutscene.forge.network;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.forge.EchoCutsceneMod;
import io.github.kxyr0.echocutscene.forge.authoring.ServerCutsceneAuthoringHost;
import io.github.kxyr0.echocutscene.forge.network.authoring.ClientboundAuthoringModePacket;
import io.github.kxyr0.echocutscene.forge.network.authoring.ClientboundAuthoringStartPacket;
import io.github.kxyr0.echocutscene.forge.network.authoring.ClientboundAuthoringStopPacket;
import io.github.kxyr0.echocutscene.forge.network.authoring.ClientboundAuthoringTargetPacket;
import io.github.kxyr0.echocutscene.forge.network.authoring.ServerboundCameraRecordingSavePacket;
import io.github.kxyr0.echocutscene.forge.playback.ServerCutscenePlaybackHost;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EchoCutsceneNetwork {
    private static final String PROTOCOL = "6";
    private static SimpleChannel channel;
    private static ServerCutscenePlaybackHost host;
    private static ServerCutsceneAuthoringHost authoringHost;
    private static int nextMessageId;

    private EchoCutsceneNetwork() {
    }

    public static void register(
            ServerCutscenePlaybackHost playbackHost,
            ServerCutsceneAuthoringHost serverAuthoringHost
    ) {
        host = playbackHost;
        authoringHost = serverAuthoringHost;
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(EchoCutsceneMod.MOD_ID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        registerMessages();
    }

    public static ServerCutscenePlaybackHost host() {
        return host;
    }

    public static ServerCutsceneAuthoringHost authoringHost() {
        return authoringHost;
    }

    public static void start(
            ServerPlayer player,
            ClientboundCutsceneStartPacket packet
    ) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void stop(ServerPlayer player, CutscenePlaybackId playbackId) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundCutsceneStopPacket(playbackId));
    }

    public static void setBlack(ServerPlayer player, double canvasGroup, long timeMillis) {
        float target = (float) Math.min(1.0D, Math.max(0.0D, canvasGroup));
        channel.send(PacketDistributor.PLAYER.with(() -> player),
                new ClientboundScreenBlackPacket(target, Math.max(0L, timeMillis)));
    }

    public static void ready(CutscenePlaybackId playbackId) {
        channel.sendToServer(new ServerboundCutsceneReadyPacket(playbackId));
    }

    public static void authoringStart(
            ServerPlayer player,
            String sessionId,
            CutsceneId cutsceneId,
            CameraPose initialPose,
            double cameraSpeed
    ) {
        channel.send(PacketDistributor.PLAYER.with(() -> player),
                new ClientboundAuthoringStartPacket(sessionId, cutsceneId, initialPose, cameraSpeed));
    }

    public static void authoringStop(ServerPlayer player, String sessionId) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundAuthoringStopPacket(sessionId));
    }

    public static void authoringMode(ServerPlayer player, String sessionId, CameraControlMode mode) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundAuthoringModePacket(sessionId, mode));
    }

    public static void authoringTarget(ServerPlayer player, String sessionId, Optional<CameraTarget> target) {
        channel.send(PacketDistributor.PLAYER.with(() -> player),
                new ClientboundAuthoringTargetPacket(sessionId, target));
    }

    public static void sendToServer(Object packet) {
        channel.sendToServer(packet);
    }

    private static void registerMessages() {
        registerPlaybackMessages();
        registerAuthoringMessages();
    }

    private static void registerPlaybackMessages() {
        channel.messageBuilder(ClientboundCutsceneStartPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundCutsceneStartPacket::encode)
                .decoder(ClientboundCutsceneStartPacket::decode)
                .consumerMainThread(ClientboundCutsceneStartPacket::handle).add();
        channel.messageBuilder(ClientboundCutsceneStopPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundCutsceneStopPacket::encode)
                .decoder(ClientboundCutsceneStopPacket::decode)
                .consumerMainThread(ClientboundCutsceneStopPacket::handle).add();
        channel.messageBuilder(ClientboundScreenBlackPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundScreenBlackPacket::encode)
                .decoder(ClientboundScreenBlackPacket::decode)
                .consumerMainThread(ClientboundScreenBlackPacket::handle).add();
        channel.messageBuilder(ServerboundCutsceneSkipPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundCutsceneSkipPacket::encode)
                .decoder(ServerboundCutsceneSkipPacket::decode)
                .consumerMainThread(ServerboundCutsceneSkipPacket::handle).add();
        channel.messageBuilder(ServerboundCutsceneReadyPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundCutsceneReadyPacket::encode)
                .decoder(ServerboundCutsceneReadyPacket::decode)
                .consumerMainThread(ServerboundCutsceneReadyPacket::handle).add();
    }

    private static void registerAuthoringMessages() {
        channel.messageBuilder(ClientboundAuthoringStartPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundAuthoringStartPacket::encode)
                .decoder(ClientboundAuthoringStartPacket::decode)
                .consumerMainThread(ClientboundAuthoringStartPacket::handle).add();
        channel.messageBuilder(ClientboundAuthoringStopPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundAuthoringStopPacket::encode)
                .decoder(ClientboundAuthoringStopPacket::decode)
                .consumerMainThread(ClientboundAuthoringStopPacket::handle).add();
        channel.messageBuilder(ClientboundAuthoringModePacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundAuthoringModePacket::encode)
                .decoder(ClientboundAuthoringModePacket::decode)
                .consumerMainThread(ClientboundAuthoringModePacket::handle).add();
        channel.messageBuilder(ClientboundAuthoringTargetPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundAuthoringTargetPacket::encode)
                .decoder(ClientboundAuthoringTargetPacket::decode)
                .consumerMainThread(ClientboundAuthoringTargetPacket::handle).add();
        channel.messageBuilder(ServerboundCameraRecordingSavePacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundCameraRecordingSavePacket::encode)
                .decoder(ServerboundCameraRecordingSavePacket::decode)
                .consumerMainThread(ServerboundCameraRecordingSavePacket::handle).add();
    }
}
