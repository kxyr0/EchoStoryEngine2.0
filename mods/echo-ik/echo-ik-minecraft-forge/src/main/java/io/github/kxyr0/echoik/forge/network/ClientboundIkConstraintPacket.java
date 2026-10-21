package io.github.kxyr0.echoik.forge.network;

import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echoik.forge.runtime.IkConstraintSnapshot;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server-to-client runtime IK target update.
 */
public record ClientboundIkConstraintPacket(IkConstraintSnapshot snapshot) {
    private static final int MAX_SUBJECT = 128;

    public static void encode(ClientboundIkConstraintPacket packet, FriendlyByteBuf buffer) {
        IkConstraintSnapshot snapshot = packet.snapshot();
        buffer.writeUtf(snapshot.subjectId(), MAX_SUBJECT);
        writeVec(buffer, snapshot.targetPosition());
        writeVec(buffer, snapshot.poleTarget());
        buffer.writeDouble(snapshot.targetWeight());
        buffer.writeBoolean(snapshot.clearWhenZero());
        buffer.writeBoolean(snapshot.debugEnabled());
    }

    public static ClientboundIkConstraintPacket decode(FriendlyByteBuf buffer) {
        String subjectId = buffer.readUtf(MAX_SUBJECT);
        Optional<Vec3> target = readVec(buffer);
        Optional<Vec3> pole = readVec(buffer);
        double weight = buffer.readDouble();
        boolean clear = buffer.readBoolean();
        boolean debug = buffer.readBoolean();
        return new ClientboundIkConstraintPacket(
                new IkConstraintSnapshot(subjectId, target, pole, weight, clear, debug));
    }

    private static void writeVec(FriendlyByteBuf buffer, Optional<Vec3> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(vec -> {
            buffer.writeDouble(vec.x());
            buffer.writeDouble(vec.y());
            buffer.writeDouble(vec.z());
        });
    }

    private static Optional<Vec3> readVec(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return Optional.empty();
        }
        return Optional.of(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }
}
