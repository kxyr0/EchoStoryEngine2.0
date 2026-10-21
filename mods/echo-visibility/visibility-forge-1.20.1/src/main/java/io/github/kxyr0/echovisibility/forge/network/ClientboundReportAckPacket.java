package io.github.kxyr0.echovisibility.forge.network;

import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.forge.client.runtime.VisibilityClientRuntime;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundReportAckPacket(
        VisibilityWatchId watchId,
        VisibilityWatchGeneration generation,
        long reportSequence,
        boolean accepted,
        String message) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(watchId.value());
        buffer.writeLong(generation.value());
        buffer.writeLong(reportSequence);
        buffer.writeBoolean(accepted);
        buffer.writeUtf(message);
    }

    public static ClientboundReportAckPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundReportAckPacket(new VisibilityWatchId(buffer.readUtf()),
                new VisibilityWatchGeneration(buffer.readLong()), buffer.readLong(),
                buffer.readBoolean(), buffer.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> VisibilityClientRuntime.instance().acknowledge(this)));
        context.setPacketHandled(true);
    }
}
