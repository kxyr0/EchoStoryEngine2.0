package io.github.kxyr0.echotime.forge.network;

import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EchoTimeNetwork {
    private static final String PROTOCOL = "1";
    private final SimpleChannel channel;

    public EchoTimeNetwork() {
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation("echo_time", "snapshots"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        channel.messageBuilder(EchoTimeSnapshotPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EchoTimeSnapshotPacket::encode)
                .decoder(EchoTimeSnapshotPacket::decode)
                .consumerMainThread(EchoTimeSnapshotPacket::handle)
                .add();
    }

    public void send(ServerPlayer player, EchoTimeSnapshot snapshot) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), new EchoTimeSnapshotPacket(snapshot));
    }

    public void broadcast(EchoTimeSnapshot snapshot) {
        channel.send(PacketDistributor.ALL.noArg(), new EchoTimeSnapshotPacket(snapshot));
    }
}
