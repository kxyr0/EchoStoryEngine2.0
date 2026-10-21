package io.github.kxyr0.echomagic.forge.network.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTrainingFeedback;
import io.github.kxyr0.echomagic.forge.network.MagicClientPacketEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

public record MagicGestureTrainingStatusPacket(MagicGestureTemplateTarget target, int sampleCount,
                                                boolean complete, MagicGestureTrainingFeedback feedback) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(target); buffer.writeVarInt(sampleCount); buffer.writeBoolean(complete);
        buffer.writeEnum(feedback);
    }
    public static MagicGestureTrainingStatusPacket decode(FriendlyByteBuf buffer) {
        return new MagicGestureTrainingStatusPacket(buffer.readEnum(MagicGestureTemplateTarget.class),
                Math.min(3, buffer.readVarInt()), buffer.readBoolean(),
                buffer.readEnum(MagicGestureTrainingFeedback.class));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new MagicClientPacketEvent.TrainingStatus(this)));
        context.setPacketHandled(true);
    }
}
