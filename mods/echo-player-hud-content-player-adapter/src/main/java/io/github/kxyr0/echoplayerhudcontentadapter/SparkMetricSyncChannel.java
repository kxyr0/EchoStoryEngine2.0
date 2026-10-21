package io.github.kxyr0.echoplayerhudcontentadapter;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Adapter-owned Spark metric synchronization.
 */
final class SparkMetricSyncChannel {
    private static final String PROTOCOL = "1";
    private final SimpleChannel channel;
    private int nextMessageId;

    SparkMetricSyncChannel() {
        this.channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(EchoPlayerHudContentPlayerAdapterMod.MOD_ID, "spark_metric"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        registerMessages();
    }

    void sendTo(ServerPlayer player, SparkMetricSyncPacket packet) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private void registerMessages() {
        channel.messageBuilder(SparkMetricSyncPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SparkMetricSyncPacket::encode)
                .decoder(SparkMetricSyncPacket::decode)
                .consumerMainThread(this::handleSnapshot)
                .add();
    }

    private void handleSnapshot(SparkMetricSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> SparkMetricClientMirror.INSTANCE.apply(packet));
        context.setPacketHandled(true);
    }
}
