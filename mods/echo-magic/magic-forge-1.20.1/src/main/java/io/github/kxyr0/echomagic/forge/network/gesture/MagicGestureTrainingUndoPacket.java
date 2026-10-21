package io.github.kxyr0.echomagic.forge.network.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.forge.EchoMagicMod;
import io.github.kxyr0.echomagic.forge.network.MagicNetwork;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record MagicGestureTrainingUndoPacket(MagicGestureTemplateTarget target) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeEnum(target); }
    public static MagicGestureTrainingUndoPacket decode(FriendlyByteBuf buffer) {
        return new MagicGestureTrainingUndoPacket(buffer.readEnum(MagicGestureTemplateTarget.class));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> undo(context.getSender())); context.setPacketHandled(true);
    }
    private void undo(ServerPlayer player) {
        if (player == null) return;
        var result = EchoMagicMod.services().gestureTraining().undo(new MagicPlayerRef(player.getUUID()), target);
        MagicNetwork.send(player, new MagicGestureTrainingStatusPacket(result.target(), result.sampleCount(),
                result.complete(), result.feedback()));
    }
}
