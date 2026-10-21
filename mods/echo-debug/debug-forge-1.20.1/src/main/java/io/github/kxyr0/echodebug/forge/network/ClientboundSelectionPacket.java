package io.github.kxyr0.echodebug.forge.network;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import io.github.kxyr0.echodebug.core.identity.TriggerWandSessionId;
import io.github.kxyr0.echodebug.core.selection.TriggerBlockPoint;
import io.github.kxyr0.echodebug.core.selection.TriggerSelectionGeometry;
import io.github.kxyr0.echodebug.core.selection.TriggerSelectionSnapshot;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import io.github.kxyr0.echodebug.forge.client.EchoDebugClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundSelectionPacket(
        boolean successful,
        DebugDiagnosticCode code,
        String message,
        boolean openCreationScreen,
        TriggerSelectionSnapshot snapshot
) {
    public static ClientboundSelectionPacket from(
            DebugOperationResult<TriggerSelectionSnapshot> result,
            boolean openCreationScreen
    ) {
        return new ClientboundSelectionPacket(result.successful(), result.code(), result.message(),
                openCreationScreen, result.value().orElseThrow());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(successful);
        buffer.writeEnum(code);
        buffer.writeUtf(message);
        buffer.writeBoolean(openCreationScreen);
        writeSnapshot(buffer, snapshot);
    }

    public static ClientboundSelectionPacket decode(FriendlyByteBuf buffer) {
        boolean successful = buffer.readBoolean();
        DebugDiagnosticCode code = buffer.readEnum(DebugDiagnosticCode.class);
        String message = buffer.readUtf();
        boolean open = buffer.readBoolean();
        return new ClientboundSelectionPacket(successful, code, message, open, readSnapshot(buffer));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> EchoDebugClient.applySelection(this)));
        context.setPacketHandled(true);
    }

    private static void writeSnapshot(FriendlyByteBuf buffer, TriggerSelectionSnapshot snapshot) {
        buffer.writeUtf(snapshot.sessionId().value());
        buffer.writeUtf(snapshot.playerId());
        buffer.writeEnum(snapshot.shape());
        buffer.writeLong(snapshot.revision());
        buffer.writeBoolean(snapshot.dimension().isPresent());
        snapshot.dimension().ifPresent(buffer::writeUtf);
        buffer.writeVarInt(snapshot.points().size());
        snapshot.points().forEach(point -> writePoint(buffer, point));
    }

    private static TriggerSelectionSnapshot readSnapshot(FriendlyByteBuf buffer) {
        TriggerWandSessionId sessionId = new TriggerWandSessionId(buffer.readUtf());
        String playerId = buffer.readUtf();
        TriggerWandShapeMode shape = buffer.readEnum(TriggerWandShapeMode.class);
        long revision = buffer.readLong();
        Optional<String> dimension = buffer.readBoolean() ? Optional.of(buffer.readUtf()) : Optional.empty();
        List<TriggerBlockPoint> points = readPoints(buffer);
        return new TriggerSelectionSnapshot(sessionId, playerId, shape, dimension, points, revision,
                TriggerSelectionGeometry.from(shape, points));
    }

    private static List<TriggerBlockPoint> readPoints(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<TriggerBlockPoint> points = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            points.add(readPoint(buffer));
        }
        return List.copyOf(points);
    }

    private static void writePoint(FriendlyByteBuf buffer, TriggerBlockPoint point) {
        buffer.writeUtf(point.dimension());
        buffer.writeInt(point.x());
        buffer.writeInt(point.y());
        buffer.writeInt(point.z());
    }

    private static TriggerBlockPoint readPoint(FriendlyByteBuf buffer) {
        return new TriggerBlockPoint(buffer.readUtf(), buffer.readInt(), buffer.readInt(), buffer.readInt());
    }
}
