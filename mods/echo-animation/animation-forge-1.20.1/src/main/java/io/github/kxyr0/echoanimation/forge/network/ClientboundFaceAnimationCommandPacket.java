package io.github.kxyr0.echoanimation.forge.network;

import io.github.kxyr0.echoanimation.forge.client.dialogue.DialoguePortraitAnimationState;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundFaceAnimationCommandPacket(String subjectId, String animationId) {
    public ClientboundFaceAnimationCommandPacket {
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        animationId = Objects.requireNonNull(animationId, "animationId");
    }

    static void encode(ClientboundFaceAnimationCommandPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.subjectId());
        buffer.writeUtf(packet.animationId());
    }

    static ClientboundFaceAnimationCommandPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundFaceAnimationCommandPacket(buffer.readUtf(), buffer.readUtf());
    }

    static void handle(ClientboundFaceAnimationCommandPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context networkContext = context.get();
        networkContext.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DialoguePortraitAnimationState.playPersistentSubjectNow(packet.subjectId(), packet.animationId())));
        networkContext.setPacketHandled(true);
    }
}
