package io.github.kxyr0.echoanimation.forge.network;

import io.github.kxyr0.echoanimation.forge.client.dialogue.DialoguePortraitAnimationState;
import io.github.kxyr0.echoanimation.forge.face.BlinkTimingOverride;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundBlinkTimingCommandPacket(
        String subjectId,
        boolean clear,
        int periodTicks,
        int closeTicks,
        int holdTicks,
        int openTicks
) {
    public ClientboundBlinkTimingCommandPacket {
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        if (!clear) {
            new BlinkTimingOverride(periodTicks, closeTicks, holdTicks, openTicks);
        }
    }

    public static ClientboundBlinkTimingCommandPacket set(String subjectId, BlinkTimingOverride timing) {
        return new ClientboundBlinkTimingCommandPacket(subjectId, false, timing.periodTicks(),
                timing.closeTicks(), timing.holdTicks(), timing.openTicks());
    }

    public static ClientboundBlinkTimingCommandPacket clear(String subjectId) {
        return new ClientboundBlinkTimingCommandPacket(subjectId, true, 1, 1, 0, 1);
    }

    static void encode(ClientboundBlinkTimingCommandPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.subjectId());
        buffer.writeBoolean(packet.clear());
        buffer.writeVarInt(packet.periodTicks());
        buffer.writeVarInt(packet.closeTicks());
        buffer.writeVarInt(packet.holdTicks());
        buffer.writeVarInt(packet.openTicks());
    }

    static ClientboundBlinkTimingCommandPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundBlinkTimingCommandPacket(buffer.readUtf(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    static void handle(ClientboundBlinkTimingCommandPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context networkContext = context.get();
        networkContext.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (packet.clear()) {
                DialoguePortraitAnimationState.clearBlinkTimingNow(packet.subjectId());
            } else {
                DialoguePortraitAnimationState.setBlinkTimingNow(packet.subjectId(),
                        new BlinkTimingOverride(packet.periodTicks(), packet.closeTicks(),
                                packet.holdTicks(), packet.openTicks()));
            }
        }));
        networkContext.setPacketHandled(true);
    }
}
