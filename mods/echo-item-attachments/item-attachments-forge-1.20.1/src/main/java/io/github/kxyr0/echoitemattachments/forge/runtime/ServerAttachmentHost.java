package io.github.kxyr0.echoitemattachments.forge.runtime;

import io.github.kxyr0.echoitemattachments.api.command.MarkerSignal;
import io.github.kxyr0.echoitemattachments.api.command.MoveItemCommand;
import io.github.kxyr0.echoitemattachments.api.command.ReleaseItemCommand;
import io.github.kxyr0.echoitemattachments.api.command.TakeItemCommand;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.model.AttachmentView;
import io.github.kxyr0.echoitemattachments.api.model.TransitionKind;
import io.github.kxyr0.echoitemattachments.api.result.AttachmentCommandResult;
import io.github.kxyr0.echoitemattachments.core.command.CommandLedger;
import io.github.kxyr0.echoitemattachments.core.command.CommandReplayGuard;
import io.github.kxyr0.echoitemattachments.core.custody.AttachmentLedger;
import io.github.kxyr0.echoitemattachments.core.custody.CustodyConflictException;
import io.github.kxyr0.echoitemattachments.core.lease.HandLeaseBook;
import io.github.kxyr0.echoitemattachments.core.marker.MarkerBinding;
import io.github.kxyr0.echoitemattachments.core.marker.MarkerGate;
import io.github.kxyr0.echoitemattachments.forge.api.service.ItemAttachmentService;
import io.github.kxyr0.echoitemattachments.forge.api.service.PlaybackBinding;
import io.github.kxyr0.echoitemattachments.forge.network.AttachmentNetwork;
import io.github.kxyr0.echoitemattachments.forge.persistence.AttachmentJournal;
import io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingOperation;
import io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingTake;
import io.github.kxyr0.echoitemattachments.forge.runtime.motion.AttachmentMotionSampler;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

public final class ServerAttachmentHost implements ItemAttachmentService {
    private final MinecraftServer server;
    private final AttachmentJournal journal;
    private final AttachmentNetwork network;
    private final AttachmentLedger attachments = new AttachmentLedger();
    private final CommandLedger commands = new CommandLedger();
    private final CommandReplayGuard replay = new CommandReplayGuard();
    private final HandLeaseBook leases = new HandLeaseBook();
    private final MarkerGate markers = new MarkerGate();
    private final Map<AttachmentCommandId, PendingOperation> pending = new LinkedHashMap<>();
    private final AttachmentMotionSampler motion = new AttachmentMotionSampler();
    private final TakeFlow takes = new TakeFlow(this);
    private final MoveFlow moves = new MoveFlow(this);
    private final ReleaseFlow releases = new ReleaseFlow(this);

    public ServerAttachmentHost(MinecraftServer server, AttachmentNetwork network) {
        this.server = server;
        this.network = network;
        this.journal = new AttachmentJournal(server.getWorldPath(
                net.minecraft.world.level.storage.LevelResource.ROOT));
        journal.load().forEach(attachments::restore);
        new AttachmentRecovery(this).recover();
    }

    @Override
    public AttachmentCommandResult take(TakeItemCommand command) {
        return command(command.commandId(), "take|" + command, () -> takes.start(command));
    }

    @Override
    public AttachmentCommandResult move(MoveItemCommand command) {
        return command(command.commandId(), "move|" + command, () -> moves.start(command));
    }

    @Override
    public AttachmentCommandResult drop(ReleaseItemCommand command) {
        return command(command.commandId(), "drop|" + command,
                () -> releases.start(command, TransitionKind.DROP));
    }

    @Override
    public AttachmentCommandResult throwItem(ReleaseItemCommand command) {
        return command(command.commandId(), "throw|" + command,
                () -> releases.start(command, TransitionKind.THROW));
    }

    @Override
    public AttachmentCommandResult marker(AttachmentCommandId commandId, MarkerSignal signal) {
        return guarded(commandId, () -> new MarkerFlow(this).accept(commandId, signal));
    }

    @Override
    public AttachmentCommandResult bindPlayback(PlaybackBinding binding) {
        PendingOperation operation = pending.get(binding.commandId());
        if (operation == null) return commands.fail(binding.commandId(),
                "EIA-PLAYBACK-NO-COMMAND", "No pending command accepts this playback.");
        markers.bind(new MarkerBinding(binding.commandId(), operation.subject(), binding.animationId(),
                binding.playbackId(), binding.playbackRevision(), binding.phase(), binding.markerId()));
        return commands.running(binding.commandId(), "EIA-PLAYBACK-BOUND", "Playback marker bound.");
    }

    @Override
    public AttachmentCommandResult cancel(AttachmentCommandId commandId) {
        PendingOperation operation = pending.get(commandId);
        if (operation == null) return commands.find(commandId).orElseGet(() ->
                commands.fail(commandId, "EIA-CANCEL-NOT-FOUND", "Command does not exist."));
        if (operation instanceof PendingTake take) {
            takes.cleanup(take);
            persist();
        }
        finish(operation);
        return commands.cancel(commandId, "Pending item transition cancelled safely.");
    }

    @Override
    public Optional<AttachmentCommandResult> command(AttachmentCommandId commandId) {
        return commands.find(commandId);
    }

    @Override
    public List<AttachmentView> inspect(AttachmentSubjectRef subject) {
        return attachments.records().stream().filter(record -> record.subject().equals(subject))
                .map(record -> new AttachmentView(record.attachmentId(), record.custodyId(), record.subject(),
                        record.point(), record.itemId(), record.count(), record.state(), record.revision(),
                        pending.values().stream().filter(operation -> operation.subject().equals(subject))
                                .map(operation -> operation.commandId().value()).findFirst())).toList();
    }

    public void tick() {
        sampleAnchors();
        List.copyOf(pending.values()).stream().filter(PendingTake.class::isInstance)
                .map(PendingTake.class::cast).filter(take -> take.stage() == PendingTake.Stage.APPROACHING)
                .forEach(takes::progress);
    }

    public void recoverNow() {
        new AttachmentRecovery(this).recover();
    }

    public void dropPlayerOnDeath(UUID playerId) {
        new io.github.kxyr0.echoitemattachments.forge.runtime.death.PlayerDeathCustody(this).drop(playerId);
    }

    void persist() { journal.commit(attachments.records()); }
    void sync(AttachmentSubjectRef subject) { network.sendSubject(server, subject, attachments.records()); }
    void putPending(PendingOperation operation) { pending.put(operation.commandId(), operation); }
    Optional<PendingOperation> pending(AttachmentCommandId id) { return Optional.ofNullable(pending.get(id)); }
    void finish(PendingOperation operation) {
        pending.remove(operation.commandId());
        markers.cancel(operation.commandId());
        leases.release(operation.subject(), operation.point(), operation.commandId());
        if (operation instanceof io.github.kxyr0.echoitemattachments.forge.runtime.pending.PendingMove move) {
            leases.release(move.subject(), move.command().from(), move.commandId());
        }
    }
    public MinecraftServer server() { return server; }
    public java.util.Collection<io.github.kxyr0.echoitemattachments.core.model.CustodyRecord> records() { return attachments.records(); }
    public java.util.Collection<PendingOperation> operations() { return List.copyOf(pending.values()); }
    public MarkerGate markerGate() { return markers; }
    public boolean hasItemTransition(AttachmentSubjectRef subject) { return pending.values().stream().anyMatch(value -> value.subject().equals(subject)); }
    AttachmentLedger attachments() { return attachments; }
    CommandLedger commands() { return commands; }
    HandLeaseBook leases() { return leases; }
    MarkerGate markers() { return markers; }
    MoveFlow moves() { return moves; }
    ReleaseFlow releases() { return releases; }
    TakeFlow takes() { return takes; }
    public Optional<io.github.kxyr0.echoitemattachments.core.motion.Vector3d> handVelocity(
            io.github.kxyr0.echoitemattachments.api.identity.AttachmentId id) {
        return motion.velocity(id);
    }

    private void sampleAnchors() {
        motion.sample(server, attachments.records());
    }

    private AttachmentCommandResult guarded(AttachmentCommandId id, Operation operation) {
        AttachmentCommandResult existing = commands.find(id).orElse(null);
        if (existing != null && existing.terminal()) return existing;
        try {
            commands.accept(id);
            return operation.run();
        } catch (CustodyConflictException exception) {
            cleanupFailed(id);
            return commands.fail(id, exception.diagnosticCode(), exception.getMessage());
        } catch (RuntimeException exception) {
            cleanupFailed(id);
            return commands.fail(id, "EIA-INTERNAL", exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private AttachmentCommandResult command(AttachmentCommandId id, String fingerprint, Operation operation) {
        if (!replay.accepts(id, fingerprint)) return commands.conflict(id);
        return commands.find(id).orElseGet(() -> guarded(id, operation));
    }

    private void cleanupFailed(AttachmentCommandId id) {
        leases.releaseCommand(id);
        PendingOperation operation = pending.remove(id);
        markers.cancel(id);
        if (operation instanceof PendingTake take) takes.cleanup(take);
        if (operation != null) persist();
    }

    @FunctionalInterface
    private interface Operation { AttachmentCommandResult run(); }
}
