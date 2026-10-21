package io.github.kxyr0.echodebug.forge.network;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.identity.DebugRequestId;
import io.github.kxyr0.echodebug.forge.creation.TriggerCreationResult;
import io.github.kxyr0.echodebug.forge.creation.TriggerCreationService;
import io.github.kxyr0.echodebug.forge.wand.TriggerWandUseGuard;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundCreateTriggerPacket(
        String requestId,
        long expectedRevision,
        String namespace,
        String path,
        String story
) {
    private static final TriggerWandUseGuard GUARD = new TriggerWandUseGuard();

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(requestId);
        buffer.writeLong(expectedRevision);
        buffer.writeUtf(namespace);
        buffer.writeUtf(path);
        buffer.writeUtf(story);
    }

    public static ServerboundCreateTriggerPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundCreateTriggerPacket(buffer.readUtf(), buffer.readLong(),
                buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
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
        DebugDiagnosticCode guard = GUARD.check(player);
        TriggerCreationResult result = guard == DebugDiagnosticCode.OK ? create(player)
                : TriggerCreationResult.failure(guard, guard.name());
        EchoDebugNetwork.sendCreationResult(player, result);
    }

    private TriggerCreationResult create(ServerPlayer player) {
        var session = EchoDebugNetwork.sessions().session(player);
        var selection = session.snapshot();
        if (selection.revision() != expectedRevision) {
            return TriggerCreationResult.failure(DebugDiagnosticCode.DEBUG_SELECTION_REVISION_STALE,
                    "Selection revision changed before creation.");
        }
        TriggerCreationResult result = EchoDebugNetwork.creationService()
                .create(player.getServer(), selection, requestId,
                        new TriggerCreationService.Target(namespace, path, story));
        if (result.successful()) {
            EchoDebugNetwork.sendSelection(player, session.clear(new DebugRequestId(requestId + ":clear"),
                    session.snapshot().revision()), false);
            EchoDebugNetwork.sendCatalog(player);
        }
        return result;
    }
}
