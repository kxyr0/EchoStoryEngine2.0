package io.github.kxyr0.echovisibility.forge.network;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.state.VisibilityConcealmentStatus;
import io.github.kxyr0.echovisibility.forge.client.runtime.VisibilityClientRuntime;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundConcealmentPacket(
        VisibilityConcealmentLeaseId leaseId,
        VisibilityConcealmentStatus status,
        String message) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(leaseId.value());
        buffer.writeEnum(status);
        buffer.writeUtf(message);
    }

    public static ClientboundConcealmentPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundConcealmentPacket(new VisibilityConcealmentLeaseId(buffer.readUtf()),
                buffer.readEnum(VisibilityConcealmentStatus.class), buffer.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> VisibilityClientRuntime.instance().concealment(this)));
        context.setPacketHandled(true);
    }
}
