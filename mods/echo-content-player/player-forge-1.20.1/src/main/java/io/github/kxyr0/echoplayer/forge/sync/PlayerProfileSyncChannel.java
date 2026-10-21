package io.github.kxyr0.echoplayer.forge.sync;

import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.forge.EchoContentPlayerMod;
import io.github.kxyr0.echoplayer.forge.client.PlayerClientMirror;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Server-authoritative profile snapshot synchronization.
 */
public final class PlayerProfileSyncChannel {
    private static final String PROTOCOL = "1";
    private final SimpleChannel channel;
    private int nextMessageId;

    public PlayerProfileSyncChannel() {
        this.channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(EchoContentPlayerMod.MOD_ID,
                "profile"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        registerMessages();
    }

    public void sendTo(ServerPlayer player, PlayerProfileSnapshot snapshot) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), PlayerProfileSyncPacket.from(snapshot));
    }

    public void sendToAll(PlayerProfileSnapshot snapshot) {
        channel.send(PacketDistributor.ALL.noArg(), PlayerProfileSyncPacket.from(snapshot));
    }

    private void registerMessages() {
        channel.messageBuilder(PlayerProfileSyncPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerProfileSyncPacket::encode)
                .decoder(PlayerProfileSyncPacket::decode)
                .consumerMainThread(this::handleSnapshot)
                .add();
    }

    private void handleSnapshot(PlayerProfileSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> PlayerClientMirror.INSTANCE.apply(packet));
        context.setPacketHandled(true);
    }
}
