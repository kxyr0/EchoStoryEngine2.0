package io.github.kxyr0.echoplayerfollow.network;

import io.github.kxyr0.echoplayerfollow.client.EchoPlayerFollowClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundFollowStopPacket(String actorId, String reason) {
    public static void encode(ClientboundFollowStopPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.actorId());
        buffer.writeUtf(packet.reason());
    }

    public static ClientboundFollowStopPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundFollowStopPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(ClientboundFollowStopPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context actual = context.get();
        actual.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoPlayerFollowClient.stop(packet)));
        actual.setPacketHandled(true);
    }
}
