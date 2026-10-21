package io.github.kxyr0.echocutscene.forge.network.authoring;

import io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundAuthoringStopPacket(String sessionId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId);
    }

    public static ClientboundAuthoringStopPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundAuthoringStopPacket(buffer.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoCutsceneClient.endAuthoring(sessionId)));
        context.setPacketHandled(true);
    }
}
