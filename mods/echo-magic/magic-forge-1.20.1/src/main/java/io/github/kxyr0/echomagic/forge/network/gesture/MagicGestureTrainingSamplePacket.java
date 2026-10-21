package io.github.kxyr0.echomagic.forge.network.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureRecognizer;
import io.github.kxyr0.echomagic.forge.EchoMagicMod;
import io.github.kxyr0.echomagic.forge.network.MagicNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record MagicGestureTrainingSamplePacket(MagicGestureTemplateTarget target,
                                                List<MagicGesturePoint> points) {
    public MagicGestureTrainingSamplePacket { points = List.copyOf(points); }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(target); buffer.writeVarInt(points.size());
        points.forEach(point -> {
            buffer.writeDouble(point.x()); buffer.writeDouble(point.y());
            buffer.writeVarLong(point.elapsedMillis());
        });
    }

    public static MagicGestureTrainingSamplePacket decode(FriendlyByteBuf buffer) {
        MagicGestureTemplateTarget target = buffer.readEnum(MagicGestureTemplateTarget.class);
        int size = buffer.readVarInt();
        if (size < 2 || size > MagicGestureRecognizer.MAXIMUM_POINTS) {
            throw new IllegalArgumentException("Invalid training gesture point count: " + size);
        }
        List<MagicGesturePoint> points = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            points.add(new MagicGesturePoint(buffer.readDouble(), buffer.readDouble(), buffer.readVarLong()));
        }
        return new MagicGestureTrainingSamplePacket(target, points);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> accept(context.getSender())); context.setPacketHandled(true);
    }

    private void accept(ServerPlayer player) {
        if (player == null) return;
        MagicPlayerRef ref = new MagicPlayerRef(player.getUUID());
        var result = EchoMagicMod.services().gestureTraining().sample(ref, target, points);
        if (result.complete()) EchoMagicMod.services().persistPlayer(ref);
        MagicNetwork.send(player, new MagicGestureTrainingStatusPacket(result.target(),
                result.sampleCount(), result.complete(), result.feedback()));
    }
}
