package io.github.kxyr0.echoplayerfollow.network;

import io.github.kxyr0.echoplayerfollow.client.EchoPlayerFollowClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundFollowStartPacket(int entityId, String actorId, String displayName) {
    public static void encode(ClientboundFollowStartPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeUtf(packet.actorId());
        buffer.writeUtf(packet.displayName());
    }

    public static ClientboundFollowStartPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundFollowStartPacket(buffer.readVarInt(), buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(ClientboundFollowStartPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context actual = context.get();
        actual.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoPlayerFollowClient.start(packet)));
        actual.setPacketHandled(true);
    }
}
