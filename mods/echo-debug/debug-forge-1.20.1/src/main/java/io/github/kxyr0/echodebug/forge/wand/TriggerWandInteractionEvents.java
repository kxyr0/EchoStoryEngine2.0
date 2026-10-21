package io.github.kxyr0.echodebug.forge.wand;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import io.github.kxyr0.echodebug.core.identity.DebugRequestId;
import io.github.kxyr0.echodebug.core.request.DebugOperationKind;
import io.github.kxyr0.echodebug.core.selection.TriggerBlockPoint;
import io.github.kxyr0.echodebug.core.selection.TriggerSelectionSnapshot;
import io.github.kxyr0.echodebug.core.selection.TriggerWandSession;
import io.github.kxyr0.echodebug.forge.network.EchoDebugNetwork;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TriggerWandInteractionEvents {
    private final TriggerWandSessionStore sessions;
    private final TriggerWandUseGuard guard = new TriggerWandUseGuard();

    public TriggerWandInteractionEvents(TriggerWandSessionStore sessions) {
        this.sessions = sessions;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!TriggerWandUseGuard.held(event.getEntity())) {
            return;
        }
        cancel(event);
        if (event.getEntity() instanceof ServerPlayer player) {
            addPoint(player, event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onLeftClickEntity(AttackEntityEvent event) {
        if (!TriggerWandUseGuard.held(event.getEntity())) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            addPoint(player, event.getTarget().blockPosition());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (TriggerWandUseGuard.held(event.getEntity())) {
            cancel(event);
            handleRightClick(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (TriggerWandUseGuard.held(event.getEntity())) {
            cancel(event);
            handleRightClick(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (TriggerWandUseGuard.held(event.getEntity())) {
            cancel(event);
            handleRightClick(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRightClickEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (TriggerWandUseGuard.held(event.getEntity())) {
            cancel(event);
            handleRightClick(event.getEntity());
        }
    }

    private void addPoint(ServerPlayer player, BlockPos pos) {
        TriggerWandSession session = sessions.session(player);
        DebugDiagnosticCode checked = guard.check(player);
        var result = checked == DebugDiagnosticCode.OK
                ? session.addPoint(requestId(), session.snapshot().revision(), point(player, pos),
                        new Vector3(player.getX(), player.getY(), player.getZ()))
                : session.failure(checked, checked.name());
        EchoDebugNetwork.sendSelection(player, result, false);
        EchoDebugNetwork.sendCatalog(player);
    }

    private void handleRightClick(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            TriggerWandSession session = sessions.session(serverPlayer);
            boolean clear = serverPlayer.isShiftKeyDown();
            DebugDiagnosticCode checked = guard.check(serverPlayer);
            var result = checked == DebugDiagnosticCode.OK ? edit(session, clear)
                    : session.failure(checked, checked.name());
            EchoDebugNetwork.sendSelection(serverPlayer, result, false);
            EchoDebugNetwork.sendCatalog(serverPlayer);
        }
    }

    private static DebugOperationResult<TriggerSelectionSnapshot> edit(
            TriggerWandSession session,
            boolean clear
    ) {
        DebugRequestId id = requestId();
        return clear ? session.clear(id, session.snapshot().revision())
                : session.undo(id, session.snapshot().revision());
    }

    private static TriggerBlockPoint point(ServerPlayer player, BlockPos pos) {
        String dimension = player.level().dimension().location().toString();
        return new TriggerBlockPoint(dimension, pos.getX(), pos.getY(), pos.getZ());
    }

    private static DebugRequestId requestId() {
        return new DebugRequestId(DebugOperationKind.ADD_POINT + ":" + UUID.randomUUID());
    }

    private static void cancel(PlayerInteractEvent event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
