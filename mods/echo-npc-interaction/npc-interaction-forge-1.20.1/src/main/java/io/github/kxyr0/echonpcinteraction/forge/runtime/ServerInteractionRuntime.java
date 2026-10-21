package io.github.kxyr0.echonpcinteraction.forge.runtime;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionOwnerId;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionOutcome;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import io.github.kxyr0.echonpcinteraction.core.command.ActiveNpcInteractions;
import io.github.kxyr0.echonpcinteraction.core.receipt.NpcInteractionReceiptLedger;
import io.github.kxyr0.echonpcinteraction.core.validate.NpcInteractionRequestValidator;
import io.github.kxyr0.echonpcinteraction.forge.alignment.NpcInteractionAlignment;
import io.github.kxyr0.echonpcinteraction.forge.api.execution.NpcInteractionExecutionPort;
import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProviderRegistry;
import io.github.kxyr0.echonpcinteraction.forge.api.service.NpcInteractionQueryService;
import io.github.kxyr0.echonpcinteraction.forge.api.service.NpcInteractionService;
import io.github.kxyr0.echonpcinteraction.forge.api.view.NpcInteractionDiagnosticsView;
import io.github.kxyr0.echonpcinteraction.forge.seat.NpcSeatOccupancyRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

public final class ServerInteractionRuntime implements NpcInteractionService, NpcInteractionQueryService {
    private final MinecraftServer server;
    private final NpcInteractionProviderRegistry providers;
    private final Supplier<List<NpcInteractionExecutionPort>> ports;
    private final Map<NpcInteractionCommandId, InteractionRecord> records = new LinkedHashMap<>();
    private final Map<NpcInteractionCommandId, NpcInteractionResult> results = new LinkedHashMap<>();
    private final ActiveNpcInteractions active = new ActiveNpcInteractions();
    private final NpcInteractionReceiptLedger receipts = new NpcInteractionReceiptLedger();
    private final InteractionDiagnosticsLog diagnostics = new InteractionDiagnosticsLog();
    private final NpcSeatOccupancyRegistry occupancies;

    public ServerInteractionRuntime(MinecraftServer server, NpcInteractionProviderRegistry providers,
            Supplier<List<NpcInteractionExecutionPort>> ports, NpcSeatOccupancyRegistry occupancies) {
        this.server = server;
        this.providers = providers;
        this.ports = ports;
        this.occupancies = occupancies;
    }

    @Override
    public NpcInteractionResult interact(NpcInteractionRequest request) {
        Optional<NpcInteractionResult> replay = receipts.find(request.commandId())
                .map(receipt -> NpcInteractionResult.completed(receipt, "Receipt replayed."));
        if (replay.isPresent()) {
            return remember(replay.orElseThrow());
        }
        Optional<NpcInteractionFailure> invalid = new NpcInteractionRequestValidator().validate(request);
        if (invalid.isPresent()) {
            return fail(request, NpcInteractionOutcome.TARGET_INVALID, invalid.orElseThrow(), "Request rejected.");
        }
        if (!active.reserve(request.npcId(), request.commandId())) {
            return fail(request, NpcInteractionOutcome.INTERNAL_FAILURE,
                    NpcInteractionFailure.NPC_INTERACTION_ALREADY_ACTIVE, "NPC already has active interaction.");
        }
        StartOutcome started = new InteractionStartFlow(server, providers, ports).start(request);
        if (started.record().isEmpty()) {
            active.release(request.npcId(), request.commandId());
            logFailure(started.result());
        }
        started.record().ifPresent(record -> records.put(request.commandId(), record));
        return remember(started.result());
    }

    @Override
    public NpcInteractionResult stop(ActorId npcId, NpcInteractionOwnerId ownerId) {
        Optional<NpcInteractionCommandId> activeId = active.active(npcId);
        Optional<InteractionRecord> record = activeId.map(records::get);
        record.flatMap(InteractionRecord::moveId)
                .ifPresent(moveId -> port().ifPresent(value -> value.cancelNavigation(server, moveId)));
        activeId.ifPresent(id -> active.release(npcId, id));
        activeId.ifPresent(records::remove);
        return activeId.map(id -> remember(new NpcInteractionResult(id, NpcInteractionStatus.CANCELLED,
                NpcInteractionOutcome.CANCELLED, Optional.empty(), Optional.empty(), Optional.empty(),
                "Interaction cancelled."))).orElseGet(() -> NpcInteractionResult.failed(
                new NpcInteractionCommandId(ownerId.value() + "_stop"), NpcInteractionOutcome.CANCELLED,
                NpcInteractionFailure.NPC_INTERACTION_NPC_NOT_FOUND, "No active interaction."));
    }

    @Override
    public NpcInteractionResult standUp(ActorId npcId, NpcInteractionOwnerId ownerId) {
        return remember(new NpcStandUpFlow(occupancies).standUp(server, npcId, ownerId));
    }

    public void tick() {
        for (InteractionRecord record : records.values().stream().toList()) {
            if (record.status() == NpcInteractionStatus.WAITING_FOR_NAVIGATION) {
                tickNavigation(record);
            }
        }
    }

    @Override
    public Optional<NpcInteractionResult> active(ActorId npcId) {
        return active.active(npcId).flatMap(this::command);
    }

    @Override
    public Optional<NpcInteractionResult> command(NpcInteractionCommandId commandId) {
        if (records.containsKey(commandId)) {
            InteractionRecord record = records.get(commandId);
            return Optional.of(NpcInteractionResult.running(commandId, record.status(), "Interaction is active."));
        }
        return Optional.ofNullable(results.get(commandId));
    }

    @Override
    public List<NpcInteractionResult> list(int limit) {
        return results.values().stream().limit(Math.max(0, limit)).toList();
    }

    @Override
    public NpcInteractionDiagnosticsView diagnostics() {
        return diagnostics.view();
    }

    private void tickNavigation(InteractionRecord record) {
        Optional<NpcInteractionExecutionPort> executionPort = port();
        if (executionPort.isEmpty() || record.moveId().isEmpty()) {
            complete(record, fail(record.request(), NpcInteractionOutcome.NAVIGATION_FAILED,
                    NpcInteractionFailure.NPC_INTERACTION_NAVIGATION_FAILED, "Navigation observation unavailable."));
            return;
        }
        executionPort.orElseThrow().navigation(server, record.moveId().orElseThrow()).ifPresent(observation -> {
            if (observation.arrived()) {
                execute(record, executionPort.orElseThrow());
            } else if (observation.terminal()) {
                complete(record, fail(record.request(), NpcInteractionOutcome.NAVIGATION_FAILED,
                        NpcInteractionFailure.NPC_INTERACTION_NAVIGATION_FAILED, observation.message()));
            }
        });
    }

    private void execute(InteractionRecord record, NpcInteractionExecutionPort executionPort) {
        var body = io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge
                .lookup(server, record.request().npcId()).body();
        if (body.isEmpty() || !new NpcInteractionAlignment().align(body.orElseThrow(), record.plan().approaches().get(0))) {
            complete(record, fail(record.request(), NpcInteractionOutcome.ALIGNMENT_FAILED,
                    NpcInteractionFailure.NPC_INTERACTION_ALIGNMENT_FAILED, "Alignment failed."));
            return;
        }
        var provider = providers.find(record.plan().providerId());
        complete(record, provider.map(value -> value.execute(server, executionPort, record.request(), record.plan()))
                .orElseGet(() -> fail(record.request(), NpcInteractionOutcome.NO_PROVIDER,
                        NpcInteractionFailure.NPC_INTERACTION_PROVIDER_NOT_FOUND, "Provider disappeared.")));
    }

    private NpcInteractionResult fail(NpcInteractionRequest request, NpcInteractionOutcome outcome,
            NpcInteractionFailure failure, String message) {
        diagnostics.add(failure.name(), "forge-runtime", message, server.overworld().getGameTime(),
                Map.of("commandId", request.commandId().value(), "npcId", request.npcId().value()));
        return remember(NpcInteractionResult.failed(request.commandId(), outcome, failure, message));
    }

    private void complete(InteractionRecord record, NpcInteractionResult result) {
        result.receipt().ifPresent(receipts::save);
        records.remove(result.commandId());
        active.release(record.request().npcId(), result.commandId());
        logFailure(result);
        remember(result);
    }

    private void logFailure(NpcInteractionResult result) {
        result.failure().ifPresent(failure -> diagnostics.add(failure.name(), "forge-runtime", result.message(),
                server.overworld().getGameTime(), Map.of("commandId", result.commandId().value())));
    }

    private NpcInteractionResult remember(NpcInteractionResult result) {
        results.put(result.commandId(), result);
        result.receipt().ifPresent(receipts::save);
        return result;
    }

    private Optional<NpcInteractionExecutionPort> port() {
        return ports.get().stream().findFirst();
    }
}
