package io.github.kxyr0.echoteleportmagic.forge.server;

import io.github.kxyr0.echoteleportmagic.api.TeleportAnchor;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecution;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecutionId;
import io.github.kxyr0.echoteleportmagic.api.TeleportState;
import io.github.kxyr0.echoteleportmagic.forge.network.TeleportNetwork;
import io.github.kxyr0.echoteleportmagic.forge.network.TeleportVisualPacket;
import io.github.kxyr0.echoteleportmagic.forge.persistence.TeleportReceiptStore;
import io.github.kxyr0.echoteleportmagic.runtime.TeleportPort;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

public final class ForgeTeleportPort implements TeleportPort {
    private final MinecraftServer server;
    private final DestinationPreparation preparation;
    private final TeleportReceiptStore receipts;
    private final Consumer<TeleportExecution> completion;
    private final Consumer<TeleportExecution> lifecycle;
    private final java.util.Map<java.util.UUID, java.util.Set<java.util.UUID>> viewers = new java.util.HashMap<>();
    private final io.github.kxyr0.echoteleportmagic.forge.integration.NpcTeleportCommit npc;

    public ForgeTeleportPort(MinecraftServer server, TeleportReceiptStore receipts,
            Consumer<TeleportExecution> completion, Consumer<TeleportExecution> lifecycle) {
        this.server = server;
        this.receipts = receipts;
        this.completion = completion;
        this.lifecycle = lifecycle;
        preparation = new DestinationPreparation(server);
        npc = net.minecraftforge.fml.ModList.get().isLoaded("echo_npc")
                ? new io.github.kxyr0.echoteleportmagic.forge.integration.NpcTeleportCommit(server) : null;
    }
    @Override public void prepare(TeleportExecution e) {
        if (npc != null && !e.preview()) npc.prepare(e);
        preparation.prepare(e);
    }
    @Override public boolean alive(TeleportExecution e) {
        try { var subject = TeleportSubjects.require(server, e.subject()); return subject.isAlive() && !subject.isRemoved(); }
        catch (IllegalStateException absent) { return false; }
    }
    @Override public TeleportAnchor anchor(TeleportExecution e) { return TeleportSubjects.anchor(TeleportSubjects.require(server, e.subject())); }
    @Override public void visualStart(TeleportExecution e) {
        broadcast(e, e.source(), e.startTick(), TeleportVisualPacket.Kind.START, false);
        if (!e.preview()) lifecycle.accept(e);
    }
    @Override public boolean commit(TeleportExecution e) {
        var entity = TeleportSubjects.require(server, e.subject());
        var target = e.destination();
        if (!entity.level().dimension().location().toString().equals(target.dimension())) {
            throw new IllegalStateException("Subject changed dimension before commit");
        }
        // Stop movement only inside concealment; buildup retains the current pose/animation/seat.
        entity.stopRiding();
        if (entity instanceof Mob mob) mob.getNavigation().stop();
        if (npc != null && npc.supports(e)) return npc.commit(e);
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(player.serverLevel(), target.x(), target.y(), target.z(), target.yaw(), target.pitch());
        } else {
            entity.teleportTo(target.x(), target.y(), target.z());
            entity.setYRot(target.yaw()); entity.setXRot(target.pitch());
        }
        entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        entity.fallDistance = 0;
        entity.getPersistentData().putUUID("echo_teleport_magic:last_commit", e.id().value());
        return true;
    }
    @Override public void visualCommit(TeleportExecution e) {
        broadcast(e, e.source(), e.startTick(), TeleportVisualPacket.Kind.COMMIT, false);
        if (!e.preview()) {
            lifecycle.accept(e);
            var d = e.destination();
            var arrival = new TeleportAnchor(d.x(), d.y(), d.z(), d.yaw(), e.source().width(), e.source().height());
            broadcast(e, arrival, server.overworld().getGameTime(), TeleportVisualPacket.Kind.START, true);
        }
    }
    @Override public void finished(TeleportExecution e) {
        preparation.release(e);
        if (npc != null) npc.release(e);
        broadcast(e, e.source(), e.startTick(), e.state() == TeleportState.COMPLETED
                ? TeleportVisualPacket.Kind.COMPLETE : TeleportVisualPacket.Kind.CANCEL, false);
        if (!e.preview()) lifecycle.accept(e);
        completion.accept(e);
        viewers.remove(e.id().value());
    }
    @Override public void receipt(TeleportExecution e, ReceiptStage stage) { if (!e.preview()) receipts.write(e, stage); }
    @Override public ReceiptStage receipt(TeleportExecutionId id) { return receipts.read(id); }

    private void broadcast(TeleportExecution e, TeleportAnchor anchor, long start,
            TeleportVisualPacket.Kind kind, boolean arrival) {
        var packet = new TeleportVisualPacket(e.id().value(), e.subject(), anchor, start, e.seed(), kind, e.preview(), arrival, -1);
        var recipients = viewers.computeIfAbsent(e.id().value(), ignored -> new java.util.HashSet<>());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean sameDimension = player.level().dimension().location().toString().equals(e.destination().dimension());
            if (recipients.contains(player.getUUID()) || sameDimension && (player.getUUID().equals(e.subject())
                    || player.distanceToSqr(anchor.x(), anchor.y(), anchor.z()) <= 256 * 256)) {
                recipients.add(player.getUUID());
                TeleportNetwork.send(player, packet);
            }
        }
    }
}
