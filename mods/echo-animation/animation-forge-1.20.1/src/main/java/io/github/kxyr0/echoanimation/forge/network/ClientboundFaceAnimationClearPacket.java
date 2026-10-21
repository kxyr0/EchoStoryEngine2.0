package io.github.kxyr0.echoanimation.forge.network;

import io.github.kxyr0.echoanimation.forge.client.dialogue.DialoguePortraitAnimationState;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundFaceAnimationClearPacket(String subjectId, boolean clearAll) {
    public ClientboundFaceAnimationClearPacket {
        subjectId = Objects.requireNonNull(subjectId, "subjectId").trim();
        if (!clearAll && subjectId.isEmpty()) {
            throw new IllegalArgumentException("subjectId must not be blank.");
        }
    }

    public static ClientboundFaceAnimationClearPacket subject(String subjectId) {
        return new ClientboundFaceAnimationClearPacket(subjectId, false);
    }

    public static ClientboundFaceAnimationClearPacket all() {
        return new ClientboundFaceAnimationClearPacket("", true);
    }

    static void encode(ClientboundFaceAnimationClearPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.clearAll());
        buffer.writeUtf(packet.subjectId());
    }

    static ClientboundFaceAnimationClearPacket decode(FriendlyByteBuf buffer) {
        boolean all = buffer.readBoolean();
        return new ClientboundFaceAnimationClearPacket(buffer.readUtf(), all);
    }

    static void handle(ClientboundFaceAnimationClearPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context networkContext = context.get();
        networkContext.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (packet.clearAll()) {
                DialoguePortraitAnimationState.clearAllPersistentSubjectsNow();
            } else {
                DialoguePortraitAnimationState.clearPersistentSubjectNow(packet.subjectId());
            }
        }));
        networkContext.setPacketHandled(true);
    }
}
