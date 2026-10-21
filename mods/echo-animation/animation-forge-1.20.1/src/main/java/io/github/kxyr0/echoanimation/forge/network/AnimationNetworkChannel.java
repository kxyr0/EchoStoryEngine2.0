package io.github.kxyr0.echoanimation.forge.network;

import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.forge.EchoAnimationMod;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forge network channel for server-authoritative animation playback cursors.
 */
public final class AnimationNetworkChannel {
    private static final String PROTOCOL = "2";
    private static final int SNAPSHOT_PACKET_ID = 0;
    private static final int REMOVE_PACKET_ID = 1;
    private static final int FACE_COMMAND_PACKET_ID = 2;
    private static final int BLINK_TIMING_COMMAND_PACKET_ID = 3;
    private static final int FACE_CLEAR_PACKET_ID = 4;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(EchoAnimationMod.MOD_ID, "sync"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private AnimationNetworkChannel() {
    }

    public static void register() {
        CHANNEL.registerMessage(SNAPSHOT_PACKET_ID,
                ClientboundAnimationSubjectSnapshotPacket.class,
                ClientboundAnimationSubjectSnapshotPacket::encode,
                ClientboundAnimationSubjectSnapshotPacket::decode,
                ClientboundAnimationSubjectSnapshotPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(REMOVE_PACKET_ID,
                ClientboundAnimationSubjectRemovePacket.class,
                ClientboundAnimationSubjectRemovePacket::encode,
                ClientboundAnimationSubjectRemovePacket::decode,
                ClientboundAnimationSubjectRemovePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(FACE_COMMAND_PACKET_ID,
                ClientboundFaceAnimationCommandPacket.class,
                ClientboundFaceAnimationCommandPacket::encode,
                ClientboundFaceAnimationCommandPacket::decode,
                ClientboundFaceAnimationCommandPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(BLINK_TIMING_COMMAND_PACKET_ID,
                ClientboundBlinkTimingCommandPacket.class,
                ClientboundBlinkTimingCommandPacket::encode,
                ClientboundBlinkTimingCommandPacket::decode,
                ClientboundBlinkTimingCommandPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(FACE_CLEAR_PACKET_ID,
                ClientboundFaceAnimationClearPacket.class,
                ClientboundFaceAnimationClearPacket::encode,
                ClientboundFaceAnimationClearPacket::decode,
                ClientboundFaceAnimationClearPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static boolean sendFullSnapshot(ServerPlayer player, AnimationSubjectSyncSnapshot snapshot) {
        return send(player, new ClientboundAnimationSubjectSnapshotPacket(snapshot, true));
    }

    public static boolean sendSnapshot(ServerPlayer player, AnimationSubjectSyncSnapshot snapshot) {
        return send(player, new ClientboundAnimationSubjectSnapshotPacket(snapshot, false));
    }

    public static boolean sendSubjectRemoval(
            ServerPlayer player,
            AnimationSubjectId subjectId,
            AnimationSyncRevision revision
    ) {
        return send(player, new ClientboundAnimationSubjectRemovePacket(subjectId, revision));
    }

    public static boolean sendFaceAnimation(ServerPlayer player, String subjectId, String animationId) {
        return send(player, new ClientboundFaceAnimationCommandPacket(subjectId, animationId));
    }

    public static void sendFaceAnimation(MinecraftServer server, String subjectId, String animationId) {
        if (server == null) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(player -> sendFaceAnimation(player, subjectId, animationId));
    }

    public static boolean sendFaceAnimationClear(ServerPlayer player, String subjectId) {
        return send(player, ClientboundFaceAnimationClearPacket.subject(subjectId));
    }

    public static boolean sendFaceAnimationClearAll(ServerPlayer player) {
        return send(player, ClientboundFaceAnimationClearPacket.all());
    }

    public static void sendFaceAnimationClear(MinecraftServer server, String subjectId) {
        if (server == null) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(player -> sendFaceAnimationClear(player, subjectId));
    }

    public static void sendFaceAnimationClearAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(AnimationNetworkChannel::sendFaceAnimationClearAll);
    }

    public static boolean sendBlinkTiming(ServerPlayer player, String subjectId,
            io.github.kxyr0.echoanimation.forge.face.BlinkTimingOverride timing) {
        return send(player, ClientboundBlinkTimingCommandPacket.set(subjectId, timing));
    }

    public static boolean sendBlinkTimingClear(ServerPlayer player, String subjectId) {
        return send(player, ClientboundBlinkTimingCommandPacket.clear(subjectId));
    }

    public static void sendBlinkTiming(MinecraftServer server, String subjectId,
            io.github.kxyr0.echoanimation.forge.face.BlinkTimingOverride timing) {
        if (server == null) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(player -> sendBlinkTiming(player, subjectId, timing));
    }

    public static void sendBlinkTimingClear(MinecraftServer server, String subjectId) {
        if (server == null) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(player -> sendBlinkTimingClear(player, subjectId));
    }

    private static boolean send(ServerPlayer player, Object packet) {
        if (player == null) {
            return false;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        return true;
    }
}
