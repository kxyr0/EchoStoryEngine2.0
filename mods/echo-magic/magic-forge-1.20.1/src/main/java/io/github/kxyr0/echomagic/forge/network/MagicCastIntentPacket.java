package io.github.kxyr0.echomagic.forge.network;

import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureRecognizer;
import io.github.kxyr0.echomagic.forge.EchoMagicMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record MagicCastIntentPacket(MagicGestureMode mode, List<List<MagicGesturePoint>> strokes) {
    private static final int MAXIMUM_STROKES = 8;

    public MagicCastIntentPacket {
        mode = Objects.requireNonNull(mode, "mode");
        strokes = strokes.stream().map(List::copyOf).toList();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(mode);
        buffer.writeVarInt(strokes.size());
        for (List<MagicGesturePoint> stroke : strokes) {
            buffer.writeVarInt(stroke.size());
            for (MagicGesturePoint point : stroke) {
                buffer.writeDouble(point.x());
                buffer.writeDouble(point.y());
                buffer.writeVarLong(point.elapsedMillis());
            }
        }
    }

    public static MagicCastIntentPacket decode(FriendlyByteBuf buffer) {
        MagicGestureMode mode = buffer.readEnum(MagicGestureMode.class);
        int strokeCount = buffer.readVarInt();
        if (strokeCount < 1 || strokeCount > MAXIMUM_STROKES) {
            throw new IllegalArgumentException("Invalid magic gesture stroke count: " + strokeCount);
        }
        int totalPoints = 0;
        List<List<MagicGesturePoint>> strokes = new ArrayList<>(strokeCount);
        for (int strokeIndex = 0; strokeIndex < strokeCount; strokeIndex++) {
            int size = buffer.readVarInt();
            totalPoints += size;
            if (size < 2 || totalPoints > MagicGestureRecognizer.MAXIMUM_POINTS) {
                throw new IllegalArgumentException("Invalid magic gesture point count: " + totalPoints);
            }
            List<MagicGesturePoint> stroke = new ArrayList<>(size);
            for (int pointIndex = 0; pointIndex < size; pointIndex++) {
                stroke.add(new MagicGesturePoint(buffer.readDouble(), buffer.readDouble(), buffer.readVarLong()));
            }
            strokes.add(stroke);
        }
        return new MagicCastIntentPacket(mode, strokes);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> accept(context.getSender()));
        context.setPacketHandled(true);
    }

    private void accept(ServerPlayer player) {
        if (player != null) EchoMagicMod.services().castGesture(player, mode, strokes);
    }
}
