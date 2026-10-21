package io.github.kxyr0.echoplayervitals.forge.sync;

import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.forge.EchoPlayerVitalsMod;
import io.github.kxyr0.echoplayervitals.forge.client.PlayerVitalsClientMirror;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Server-authoritative Vitals snapshot synchronization.
 */
public final class PlayerVitalsSyncChannel {
    private static final String PROTOCOL = "1";
    private final SimpleChannel channel;
    private int nextMessageId;

    public PlayerVitalsSyncChannel() {
        this.channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(EchoPlayerVitalsMod.MOD_ID,
                "vitals"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        registerMessages();
    }

    public void sendTo(ServerPlayer player, PlayerVitalsSnapshot snapshot) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), PlayerVitalsSyncPacket.from(snapshot));
    }

    public void sendToAll(PlayerVitalsSnapshot snapshot) {
        channel.send(PacketDistributor.ALL.noArg(), PlayerVitalsSyncPacket.from(snapshot));
    }

    private void registerMessages() {
        channel.messageBuilder(PlayerVitalsSyncPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerVitalsSyncPacket::encode)
                .decoder(PlayerVitalsSyncPacket::decode)
                .consumerMainThread(this::handleSnapshot)
                .add();
    }

    private void handleSnapshot(PlayerVitalsSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> PlayerVitalsClientMirror.INSTANCE.apply(packet));
        context.setPacketHandled(true);
    }
}
