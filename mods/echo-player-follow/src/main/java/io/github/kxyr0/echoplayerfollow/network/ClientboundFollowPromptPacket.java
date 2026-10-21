package io.github.kxyr0.echoplayerfollow.network;

import io.github.kxyr0.echoplayerfollow.client.EchoPlayerFollowClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundFollowPromptPacket(
        boolean available,
        int entityId,
        String actorId,
        String displayName,
        double distance,
        String reason
) {
    public static ClientboundFollowPromptPacket empty() {
        return empty("no_candidate");
    }

    public static ClientboundFollowPromptPacket empty(String reason) {
        return new ClientboundFollowPromptPacket(false, -1, "", "", 0.0D, reason);
    }

    public static void encode(ClientboundFollowPromptPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.available());
        if (!packet.available()) {
            buffer.writeUtf(packet.reason());
            return;
        }
        buffer.writeVarInt(packet.entityId());
        buffer.writeUtf(packet.actorId());
        buffer.writeUtf(packet.displayName());
        buffer.writeDouble(packet.distance());
    }

    public static ClientboundFollowPromptPacket decode(FriendlyByteBuf buffer) {
        boolean available = buffer.readBoolean();
        if (!available) {
            return empty(buffer.readUtf());
        }
        return new ClientboundFollowPromptPacket(true, buffer.readVarInt(), buffer.readUtf(),
                buffer.readUtf(), buffer.readDouble(), "");
    }

    public static void handle(ClientboundFollowPromptPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context actual = context.get();
        actual.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoPlayerFollowClient.prompt(packet)));
        actual.setPacketHandled(true);
    }
}
