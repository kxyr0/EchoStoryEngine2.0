package io.github.kxyr0.echoitem.forge.network;

import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import io.github.kxyr0.echoitem.forge.EchoItemPackMod;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Server-authoritative catalog sync for clients connected to the Forge runtime.
 */
public final class CatalogSyncChannel {
    private static final String PROTOCOL = "1";
    private final ItemDefinitionSynchronizer synchronizer;
    private final SimpleChannel channel;
    private int nextMessageId;

    public CatalogSyncChannel(ItemDefinitionSynchronizer synchronizer) {
        this.synchronizer = Objects.requireNonNull(synchronizer, "synchronizer");
        this.channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(EchoItemPackMod.MOD_ID, "catalog"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        registerMessages();
    }

    public void sendTo(ServerPlayer player, ItemCatalogSnapshot snapshot) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), new CatalogSnapshotMessage(snapshot));
    }

    public void sendToAll(MinecraftServer server, ItemCatalogSnapshot snapshot) {
        server.getPlayerList().getPlayers().forEach(player -> sendTo(player, snapshot));
    }

    private void registerMessages() {
        channel.messageBuilder(CatalogSnapshotMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CatalogSnapshotMessage::encode)
                .decoder(CatalogSnapshotMessage::decode)
                .consumerMainThread(this::handleSnapshot)
                .add();
    }

    private void handleSnapshot(CatalogSnapshotMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> synchronizer.applyClientSnapshot(message.snapshot()));
        context.setPacketHandled(true);
    }
}
