package io.github.kxyr0.echostoryengine.blackspace.network;

import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BlackspaceNetwork {
    private static final String PROTOCOL = "3";
    private static SimpleChannel channel;
    private static int nextMessageId;

    private BlackspaceNetwork() {
    }

    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(EchoBlackspace.MOD_ID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        channel.messageBuilder(BlackspaceSessionStatePacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(BlackspaceSessionStatePacket::encode)
                .decoder(BlackspaceSessionStatePacket::decode)
                .consumerMainThread(BlackspaceSessionStatePacket::handle).add();
        channel.messageBuilder(PrepareBlackspaceEnterPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(PrepareBlackspaceEnterPacket::encode)
                .decoder(PrepareBlackspaceEnterPacket::decode)
                .consumerMainThread(PrepareBlackspaceEnterPacket::handle).add();
        channel.messageBuilder(BlackspaceShaderReadyPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(BlackspaceShaderReadyPacket::encode)
                .decoder(BlackspaceShaderReadyPacket::decode)
                .consumerMainThread(BlackspaceShaderReadyPacket::handle).add();
        channel.messageBuilder(BlackspaceClientResetAckPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(BlackspaceClientResetAckPacket::encode)
                .decoder(BlackspaceClientResetAckPacket::decode)
                .consumerMainThread(BlackspaceClientResetAckPacket::handle).add();
    }

    public static boolean sendState(ServerPlayer player, boolean active) {
        return sendSessionState(player, active, "", 0L);
    }

    public static boolean sendSessionState(ServerPlayer player, boolean active, String blackspaceId, long resetId) {
        if (channel == null) {
            EchoBlackspace.LOGGER.warn("Echo Blackspace network channel is not registered.");
            return false;
        }
        try {
            channel.send(PacketDistributor.PLAYER.with(() -> player),
                    new BlackspaceSessionStatePacket(active, blackspaceId == null ? "" : blackspaceId, resetId));
            return true;
        } catch (RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Echo Blackspace failed to send session state active={} id={} to {}.",
                    active, blackspaceId, player.getGameProfile().getName(), exception);
            return false;
        }
    }

    public static boolean sendPrepareEnter(ServerPlayer player, long prepareId) {
        if (channel == null) {
            EchoBlackspace.LOGGER.warn("Echo Blackspace network channel is not registered.");
            return false;
        }
        try {
            channel.send(PacketDistributor.PLAYER.with(() -> player), new PrepareBlackspaceEnterPacket(prepareId));
            return true;
        } catch (RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Echo Blackspace failed to prepare shader for {}.",
                    player.getGameProfile().getName(), exception);
            return false;
        }
    }

    public static void sendShaderReady(long prepareId) {
        if (channel != null) {
            channel.sendToServer(new BlackspaceShaderReadyPacket(prepareId));
        }
    }

    public static void sendClientResetAck(long resetId) {
        if (channel != null) {
            channel.sendToServer(new BlackspaceClientResetAckPacket(resetId));
        }
    }
}
