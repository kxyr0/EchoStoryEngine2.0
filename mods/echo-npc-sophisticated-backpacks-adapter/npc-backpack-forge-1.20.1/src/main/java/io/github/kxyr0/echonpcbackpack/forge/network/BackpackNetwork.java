package io.github.kxyr0.echonpcbackpack.forge.network;

import io.github.kxyr0.echonpcbackpack.forge.EchoNpcSophisticatedBackpacksMod;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackActorRecord;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Optional;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BackpackNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(EchoNpcSophisticatedBackpacksMod.MOD_ID, "state"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private BackpackNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, BackpackSyncPacket.class, BackpackSyncPacket::encode,
                BackpackSyncPacket::decode, BackpackSyncPacket::handle);
        CHANNEL.registerMessage(1, BackpackDebugPacket.class, BackpackDebugPacket::encode,
                BackpackDebugPacket::decode, BackpackDebugPacket::handle);
    }

    public static void publish(MinecraftServer server, BackpackActorRecord record) {
        if (server != null) CHANNEL.send(PacketDistributor.ALL.noArg(), packet(record));
    }

    public static void send(ServerPlayer player, BackpackActorRecord record) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet(record));
    }

    public static void debug(ServerPlayer player, Optional<ActorId> actorId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BackpackDebugPacket(actorId));
    }

    private static BackpackSyncPacket packet(BackpackActorRecord record) {
        return new BackpackSyncPacket(record.actorId, record.payload, record.attachmentPoint,
                record.status, record.revision);
    }
}
