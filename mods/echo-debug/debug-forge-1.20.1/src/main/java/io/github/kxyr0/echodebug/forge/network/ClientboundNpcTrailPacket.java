package io.github.kxyr0.echodebug.forge.network;

import io.github.kxyr0.echodebug.core.trail.NpcTrailEvent;
import io.github.kxyr0.echodebug.core.trail.NpcTrailPoint;
import io.github.kxyr0.echodebug.core.trail.NpcTrailStatus;
import io.github.kxyr0.echodebug.forge.client.EchoDebugClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundNpcTrailPacket(
        String moveId,
        String subjectId,
        String dimension,
        double x,
        double y,
        double z,
        long serverTick,
        NpcTrailStatus status,
        long trailRevision
) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(moveId);
        buffer.writeUtf(subjectId);
        buffer.writeUtf(dimension);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeLong(serverTick);
        buffer.writeEnum(status);
        buffer.writeLong(trailRevision);
    }

    public static ClientboundNpcTrailPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundNpcTrailPacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readLong(),
                buffer.readEnum(NpcTrailStatus.class), buffer.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> EchoDebugClient.applyNpcTrail(this)));
        context.setPacketHandled(true);
    }

    public NpcTrailEvent event() {
        return new NpcTrailEvent(moveId, subjectId, dimension, new NpcTrailPoint(x, y, z, serverTick),
                status, trailRevision);
    }
}
