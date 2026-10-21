package io.github.kxyr0.echocutscene.forge.network;

import io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundScreenBlackPacket(float canvasGroup, long timeMillis) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(canvasGroup);
        buffer.writeLong(timeMillis);
    }

    public static ClientboundScreenBlackPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundScreenBlackPacket(buffer.readFloat(), buffer.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoCutsceneClient.setBlack(this)));
        context.setPacketHandled(true);
    }
}
