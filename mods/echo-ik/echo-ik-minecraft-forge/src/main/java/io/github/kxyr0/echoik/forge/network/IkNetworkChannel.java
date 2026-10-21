package io.github.kxyr0.echoik.forge.network;

import io.github.kxyr0.echoik.forge.EchoIkForgeMod;
import io.github.kxyr0.echoik.forge.runtime.IkConstraintSnapshot;
import io.github.kxyr0.echoik.forge.runtime.IkRuntimeState;
import java.util.Optional;
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
 * Forge channel for runtime IK constraints.
 */
public final class IkNetworkChannel {
    private static final String PROTOCOL = "1";
    private static final int CONSTRAINT_PACKET_ID = 0;

    private final IkRuntimeState clientState;
    private final SimpleChannel channel;

    public IkNetworkChannel(IkRuntimeState clientState) {
        this.clientState = java.util.Objects.requireNonNull(clientState, "clientState");
        this.channel = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(EchoIkForgeMod.MOD_ID, "sync"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals);
    }

    public void register() {
        channel.registerMessage(CONSTRAINT_PACKET_ID,
                ClientboundIkConstraintPacket.class,
                ClientboundIkConstraintPacket::encode,
                ClientboundIkConstraintPacket::decode,
                this::handleConstraint,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public void sendAll(MinecraftServer server, IkConstraintSnapshot snapshot) {
        if (server == null) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(player -> send(player, snapshot));
    }

    private void send(ServerPlayer player, IkConstraintSnapshot snapshot) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundIkConstraintPacket(snapshot));
    }

    private void handleConstraint(
            ClientboundIkConstraintPacket packet,
            Supplier<NetworkEvent.Context> context
    ) {
        NetworkEvent.Context value = context.get();
        value.enqueueWork(() -> clientState.apply(packet.snapshot()));
        value.setPacketHandled(true);
    }
}
