package io.github.kxyr0.echoplayerfollow.network;

import io.github.kxyr0.echoplayerfollow.EchoPlayerFollowMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FollowNetwork {
    private static final String PROTOCOL = "4";
    private static SimpleChannel channel;
    private static int nextMessageId;

    private FollowNetwork() {
    }

    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(EchoPlayerFollowMod.MOD_ID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        registerMessages();
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        channel.sendToServer(packet);
    }

    private static void registerMessages() {
        channel.messageBuilder(ClientboundFollowPromptPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundFollowPromptPacket::encode)
                .decoder(ClientboundFollowPromptPacket::decode)
                .consumerMainThread(ClientboundFollowPromptPacket::handle).add();
        channel.messageBuilder(ClientboundFollowStartPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundFollowStartPacket::encode)
                .decoder(ClientboundFollowStartPacket::decode)
                .consumerMainThread(ClientboundFollowStartPacket::handle).add();
        channel.messageBuilder(ClientboundFollowStopPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundFollowStopPacket::encode)
                .decoder(ClientboundFollowStopPacket::decode)
                .consumerMainThread(ClientboundFollowStopPacket::handle).add();
        channel.messageBuilder(ServerboundFollowStartPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundFollowStartPacket::encode)
                .decoder(ServerboundFollowStartPacket::decode)
                .consumerMainThread(ServerboundFollowStartPacket::handle).add();
        channel.messageBuilder(ServerboundFollowCancelPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundFollowCancelPacket::encode)
                .decoder(ServerboundFollowCancelPacket::decode)
                .consumerMainThread(ServerboundFollowCancelPacket::handle).add();
        channel.messageBuilder(ServerboundFollowProbePacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundFollowProbePacket::encode)
                .decoder(ServerboundFollowProbePacket::decode)
                .consumerMainThread(ServerboundFollowProbePacket::handle).add();
    }
}
