package io.github.kxyr0.echovisibility.forge.network;

import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.forge.client.runtime.VisibilityClientRuntime;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundDisarmPacket(VisibilityWatchId watchId, VisibilityWatchGeneration generation) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(watchId.value());
        buffer.writeLong(generation.value());
    }

    public static ClientboundDisarmPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundDisarmPacket(new VisibilityWatchId(buffer.readUtf()),
                new VisibilityWatchGeneration(buffer.readLong()));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> VisibilityClientRuntime.instance().disarm(this)));
        context.setPacketHandled(true);
    }
}
