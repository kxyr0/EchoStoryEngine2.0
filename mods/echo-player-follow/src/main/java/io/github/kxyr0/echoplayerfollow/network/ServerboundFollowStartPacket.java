package io.github.kxyr0.echoplayerfollow.network;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echoplayerfollow.EchoPlayerFollowMod;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundFollowStartPacket(String actorId) {
    public static void encode(ServerboundFollowStartPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.actorId());
    }

    public static ServerboundFollowStartPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundFollowStartPacket(buffer.readUtf());
    }

    public static void handle(ServerboundFollowStartPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context actual = context.get();
        Optional.ofNullable(actual.getSender()).ifPresent(sender ->
                actual.enqueueWork(() -> start(packet, sender)));
        actual.setPacketHandled(true);
    }

    private static void start(ServerboundFollowStartPacket packet, ServerPlayer sender) {
        try {
            EchoPlayerFollowMod.runtime().start(sender, new ActorId(packet.actorId()));
        } catch (IllegalArgumentException ignored) {
            EchoPlayerFollowMod.LOGGER.warn("Rejected invalid player follow ActorId from {}.", sender.getScoreboardName());
        }
    }
}
