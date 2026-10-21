package io.github.kxyr0.echovisibility.forge.network;

import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import io.github.kxyr0.echovisibility.forge.EchoVisibilityMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EchoVisibilityNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EchoVisibilityMod.MOD_ID, "main"), () -> PROTOCOL,
            PROTOCOL::equals, PROTOCOL::equals);

    private EchoVisibilityNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ClientboundArmPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundArmPacket::encode).decoder(ClientboundArmPacket::decode)
                .consumerMainThread(ClientboundArmPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundDisarmPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundDisarmPacket::encode).decoder(ClientboundDisarmPacket::decode)
                .consumerMainThread(ClientboundDisarmPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundReportPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundReportPacket::encode).decoder(ServerboundReportPacket::decode)
                .consumerMainThread(ServerboundReportPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundConcealmentPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundConcealmentPacket::encode).decoder(ClientboundConcealmentPacket::decode)
                .consumerMainThread(ClientboundConcealmentPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundReportAckPacket.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundReportAckPacket::encode).decoder(ClientboundReportAckPacket::decode)
                .consumerMainThread(ClientboundReportAckPacket::handle).add();
    }

    public static void send(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendReport(VisibilityReport report) {
        CHANNEL.sendToServer(new ServerboundReportPacket(report));
    }
}
