package io.github.kxyr0.echoanimation.forge.network;

import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.forge.client.sync.AnimationClientSyncState;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundAnimationSubjectRemovePacket(
        AnimationSubjectId subjectId,
        AnimationSyncRevision revision
) {
    public ClientboundAnimationSubjectRemovePacket {
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        revision = Objects.requireNonNull(revision, "revision");
    }

    static void encode(ClientboundAnimationSubjectRemovePacket packet, FriendlyByteBuf buffer) {
        AnimationNetworkCodec.writeSubjectId(buffer, packet.subjectId());
        AnimationNetworkCodec.writeRevision(buffer, packet.revision());
    }

    static ClientboundAnimationSubjectRemovePacket decode(FriendlyByteBuf buffer) {
        return new ClientboundAnimationSubjectRemovePacket(AnimationNetworkCodec.readSubjectId(buffer),
                AnimationNetworkCodec.readRevision(buffer));
    }

    static void handle(ClientboundAnimationSubjectRemovePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context networkContext = context.get();
        networkContext.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AnimationClientSyncState.remove(packet.subjectId(), packet.revision())));
        networkContext.setPacketHandled(true);
    }
}
