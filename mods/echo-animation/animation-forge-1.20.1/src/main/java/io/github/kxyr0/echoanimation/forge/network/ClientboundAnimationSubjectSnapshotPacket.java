package io.github.kxyr0.echoanimation.forge.network;

import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.forge.client.sync.AnimationClientSyncState;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundAnimationSubjectSnapshotPacket(AnimationSubjectSyncSnapshot snapshot, boolean full) {
    public ClientboundAnimationSubjectSnapshotPacket {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    static void encode(ClientboundAnimationSubjectSnapshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.full());
        AnimationNetworkCodec.writeSnapshot(buffer, packet.snapshot());
    }

    static ClientboundAnimationSubjectSnapshotPacket decode(FriendlyByteBuf buffer) {
        boolean full = buffer.readBoolean();
        return new ClientboundAnimationSubjectSnapshotPacket(AnimationNetworkCodec.readSnapshot(buffer), full);
    }

    static void handle(ClientboundAnimationSubjectSnapshotPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context networkContext = context.get();
        networkContext.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (packet.full()) {
                AnimationClientSyncState.applyFull(packet.snapshot());
            } else {
                AnimationClientSyncState.apply(packet.snapshot());
            }
        }));
        networkContext.setPacketHandled(true);
    }
}
