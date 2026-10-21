package io.github.kxyr0.echoplayerfollow.network;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echoplayerfollow.EchoPlayerFollowMod;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundFollowCancelPacket(String actorId, String reason) {
    public static void encode(ServerboundFollowCancelPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.actorId());
        buffer.writeUtf(packet.reason());
    }

    public static ServerboundFollowCancelPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundFollowCancelPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(ServerboundFollowCancelPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context actual = context.get();
        Optional.ofNullable(actual.getSender()).ifPresent(sender ->
                actual.enqueueWork(() -> cancel(packet, sender)));
        actual.setPacketHandled(true);
    }

    private static void cancel(ServerboundFollowCancelPacket packet, ServerPlayer sender) {
        try {
            EchoPlayerFollowMod.runtime().cancel(sender, new ActorId(packet.actorId()), packet.reason());
        } catch (IllegalArgumentException ignored) {
            EchoPlayerFollowMod.LOGGER.warn("Rejected invalid player follow cancel from {}.",
                    sender.getScoreboardName());
        }
    }
}
