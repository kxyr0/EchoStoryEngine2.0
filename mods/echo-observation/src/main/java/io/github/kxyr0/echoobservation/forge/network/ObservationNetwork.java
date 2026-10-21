package io.github.kxyr0.echoobservation.forge.network;

import io.github.kxyr0.echoobservation.forge.client.ObservationClientRuntime;
import io.github.kxyr0.echoobservation.forge.runtime.ObservationServerRuntime;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ObservationNetwork {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("echo_observation", "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ObservationNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ObservationPackets.ClientboundSnapshot.class, id++)
                .encoder(ObservationPacketCodec::writeSnapshot).decoder(ObservationPacketCodec::readSnapshot)
                .consumerMainThread(ObservationNetwork::clientSnapshot).add();
        CHANNEL.messageBuilder(ObservationPackets.ServerboundFocus.class, id++)
                .encoder(ObservationNetwork::writeFocus).decoder(ObservationNetwork::readFocus)
                .consumerMainThread(ObservationNetwork::serverFocus).add();
        CHANNEL.messageBuilder(ObservationPackets.ClientboundFocus.class, id++)
                .encoder(ObservationNetwork::writeFocusState).decoder(ObservationNetwork::readFocusState)
                .consumerMainThread(ObservationNetwork::clientFocus).add();
        CHANNEL.messageBuilder(ObservationPackets.ClientboundText.class, id++)
                .encoder(ObservationNetwork::writeText).decoder(ObservationNetwork::readText)
                .consumerMainThread(ObservationNetwork::clientText).add();
        CHANNEL.messageBuilder(ObservationPackets.ServerboundReload.class, id++)
                .encoder((value, buf) -> buf.writeUtf(value.observationId(), 512))
                .decoder(buf -> new ObservationPackets.ServerboundReload(buf.readUtf(512)))
                .consumerMainThread(ObservationNetwork::serverReload).add();
        CHANNEL.messageBuilder(ObservationPackets.ClientboundReload.class, id)
                .encoder(ObservationNetwork::writeReload).decoder(ObservationNetwork::readReload)
                .consumerMainThread(ObservationNetwork::clientReload).add();
    }

    public static void send(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        ObservationClientRuntime.instance().recordPacket();
        CHANNEL.sendToServer(packet);
    }

    private static void clientSnapshot(ObservationPackets.ClientboundSnapshot packet,
            Supplier<NetworkEvent.Context> context) {
        ObservationClientRuntime.instance().accept(packet);
        context.get().setPacketHandled(true);
    }

    private static void serverFocus(ObservationPackets.ServerboundFocus packet,
            Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null) ObservationServerRuntime.instance().focus(player, packet);
        context.get().setPacketHandled(true);
    }

    private static void clientFocus(ObservationPackets.ClientboundFocus packet,
            Supplier<NetworkEvent.Context> context) {
        ObservationClientRuntime.instance().accept(packet);
        context.get().setPacketHandled(true);
    }

    private static void clientText(ObservationPackets.ClientboundText packet,
            Supplier<NetworkEvent.Context> context) {
        ObservationClientRuntime.instance().accept(packet);
        context.get().setPacketHandled(true);
    }

    private static void serverReload(ObservationPackets.ServerboundReload packet,
            Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null) ObservationServerRuntime.instance().reload(player, packet);
        context.get().setPacketHandled(true);
    }

    private static void clientReload(ObservationPackets.ClientboundReload packet,
            Supplier<NetworkEvent.Context> context) {
        ObservationClientRuntime.instance().accept(packet);
        context.get().setPacketHandled(true);
    }

    private static void writeFocus(ObservationPackets.ServerboundFocus value, FriendlyByteBuf buf) {
        buf.writeEnum(value.kind()); buf.writeUtf(value.observationId(), 512); buf.writeVarLong(value.definitionRevision());
    }

    private static ObservationPackets.ServerboundFocus readFocus(FriendlyByteBuf buf) {
        return new ObservationPackets.ServerboundFocus(buf.readEnum(ObservationPackets.FocusKind.class),
                buf.readUtf(512), buf.readVarLong());
    }

    private static void writeFocusState(ObservationPackets.ClientboundFocus value, FriendlyByteBuf buf) {
        buf.writeEnum(value.status()); buf.writeUtf(value.observationId(), 512);
        buf.writeVarInt(value.requiredTicks()); buf.writeUtf(value.message(), 1024);
    }

    private static ObservationPackets.ClientboundFocus readFocusState(FriendlyByteBuf buf) {
        return new ObservationPackets.ClientboundFocus(buf.readEnum(ObservationPackets.FocusStatus.class),
                buf.readUtf(512), buf.readVarInt(), buf.readUtf(1024));
    }

    private static void writeText(ObservationPackets.ClientboundText value, FriendlyByteBuf buf) {
        buf.writeUtf(value.observationId(), 512); buf.writeUtf(value.text(), 8192); buf.writeVarLong(value.expiresAtMillis());
    }

    private static ObservationPackets.ClientboundText readText(FriendlyByteBuf buf) {
        return new ObservationPackets.ClientboundText(buf.readUtf(512), buf.readUtf(8192), buf.readVarLong());
    }

    private static void writeReload(ObservationPackets.ClientboundReload value, FriendlyByteBuf buf) {
        buf.writeUtf(value.observationId(), 512); buf.writeBoolean(value.successful());
        buf.writeVarLong(value.catalogRevision()); buf.writeUtf(value.message(), 1024);
    }

    private static ObservationPackets.ClientboundReload readReload(FriendlyByteBuf buf) {
        return new ObservationPackets.ClientboundReload(buf.readUtf(512), buf.readBoolean(),
                buf.readVarLong(), buf.readUtf(1024));
    }
    public static void reject(ServerPlayer player, String id, String message) {
        ObservationNetwork.send(player, new ObservationPackets.ClientboundFocus(
                ObservationPackets.FocusStatus.REJECTED, id, 0, message));
    }

    public static void focusState(ServerPlayer player, ObservationPackets.FocusStatus status,
            io.github.kxyr0.echoobservation.api.identity.ObservationId id, int ticks, String message) {
        ObservationNetwork.send(player, new ObservationPackets.ClientboundFocus(status, id.toString(), ticks, message));
    }

}
