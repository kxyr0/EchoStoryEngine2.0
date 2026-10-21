package io.github.kxyr0.echodebug.forge.network;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import io.github.kxyr0.echodebug.core.identity.DebugRequestId;
import io.github.kxyr0.echodebug.core.request.DebugOperationKind;
import io.github.kxyr0.echodebug.core.selection.TriggerSelectionSnapshot;
import io.github.kxyr0.echodebug.core.selection.TriggerWandSession;
import io.github.kxyr0.echodebug.forge.wand.TriggerWandUseGuard;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundSelectionActionPacket(
        DebugOperationKind operation,
        String requestId,
        long expectedRevision,
        boolean reverse
) {
    private static final TriggerWandUseGuard GUARD = new TriggerWandUseGuard();

    public static ServerboundSelectionActionPacket changeShape(String requestId, long revision, boolean reverse) {
        return new ServerboundSelectionActionPacket(DebugOperationKind.CHANGE_SHAPE, requestId, revision, reverse);
    }

    public static ServerboundSelectionActionPacket finalizeSelection(String requestId, long revision) {
        return new ServerboundSelectionActionPacket(DebugOperationKind.FINALIZE_SELECTION, requestId, revision, false);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(operation);
        buffer.writeUtf(requestId);
        buffer.writeLong(expectedRevision);
        buffer.writeBoolean(reverse);
    }

    public static ServerboundSelectionActionPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundSelectionActionPacket(buffer.readEnum(DebugOperationKind.class),
                buffer.readUtf(), buffer.readLong(), buffer.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handle(context.getSender()));
        context.setPacketHandled(true);
    }

    private void handle(ServerPlayer player) {
        if (player == null) {
            return;
        }
        TriggerWandSession session = EchoDebugNetwork.sessions().session(player);
        DebugDiagnosticCode guard = GUARD.check(player);
        DebugOperationResult<TriggerSelectionSnapshot> result = guard == DebugDiagnosticCode.OK
                ? apply(session) : session.failure(guard, guard.name());
        EchoDebugNetwork.sendSelection(player, result, openScreen(result));
        EchoDebugNetwork.sendCatalog(player);
    }

    private DebugOperationResult<TriggerSelectionSnapshot> apply(TriggerWandSession session) {
        DebugRequestId id = new DebugRequestId(requestId);
        return switch (operation) {
            case CHANGE_SHAPE -> session.changeShape(id, expectedRevision, reverse);
            case FINALIZE_SELECTION -> session.finalizeSelection(id, expectedRevision);
            case UNDO_POINT -> session.undo(id, expectedRevision);
            case CLEAR_SELECTION -> session.clear(id, expectedRevision);
            default -> session.failure(DebugDiagnosticCode.DEBUG_SELECTION_STAGE_INVALID,
                    "Unsupported key operation: " + operation);
        };
    }

    private boolean openScreen(DebugOperationResult<TriggerSelectionSnapshot> result) {
        return operation == DebugOperationKind.FINALIZE_SELECTION && result.successful()
                && result.value().orElseThrow().complete();
    }
}
