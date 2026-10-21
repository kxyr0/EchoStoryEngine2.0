package io.github.kxyr0.echoplayerfollow.network;

import io.github.kxyr0.echoplayerfollow.EchoPlayerFollowMod;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundFollowProbePacket(int targetEntityId) {
    public static void encode(ServerboundFollowProbePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId());
    }

    public static ServerboundFollowProbePacket decode(FriendlyByteBuf buffer) {
        return new ServerboundFollowProbePacket(buffer.readVarInt());
    }

    public static void handle(ServerboundFollowProbePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context actual = context.get();
        Optional.ofNullable(actual.getSender()).ifPresent(sender ->
                actual.enqueueWork(() -> probe(packet, sender)));
        actual.setPacketHandled(true);
    }

    private static void probe(ServerboundFollowProbePacket packet, ServerPlayer sender) {
        EchoPlayerFollowMod.runtime().probe(sender, packet.targetEntityId());
    }
}
