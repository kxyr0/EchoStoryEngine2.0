package io.github.kxyr0.echostoryengine.story.forge.host.recovery;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.execution.ExternalCommandSink;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.host.execution.ActiveStoryDefinitions;
import io.github.kxyr0.echostoryengine.story.forge.persistence.PersistenceWorkQueue;
import io.github.kxyr0.echostoryengine.story.persistence.recovery.ExecutionRecoveryPlanner;
import io.github.kxyr0.echostoryengine.story.persistence.recovery.RecoveryResult;
import io.github.kxyr0.echostoryengine.story.persistence.recovery.RecoveryStatus;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.extension.forge.lifecycle.EchoStoryExecutionTerminalEvent;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Restores durable WAITING checkpoints after extension sessions are ready.
 */
public final class StoryRecoveryDispatch {
    private final MinecraftServer server;
    private final ExecutionStateRepository repository;
    private final PersistenceWorkQueue persistenceQueue;
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;
    private final StoryCatalogState catalogState;
    private final ActiveStoryDefinitions activeDefinitions;
    private final ExternalCommandSink commandSink;
    private final ExecutionRecoveryPlanner recoveryPlanner = new ExecutionRecoveryPlanner();

    public StoryRecoveryDispatch(
            MinecraftServer server,
            ExecutionStateRepository repository,
            PersistenceWorkQueue persistenceQueue,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics,
            StoryCatalogState catalogState,
            ActiveStoryDefinitions activeDefinitions,
            ExternalCommandSink commandSink) {
        this.server = Objects.requireNonNull(server, "server");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.persistenceQueue = Objects.requireNonNull(persistenceQueue, "persistenceQueue");
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.catalogState = Objects.requireNonNull(catalogState, "catalogState");
        this.activeDefinitions = Objects.requireNonNull(activeDefinitions, "activeDefinitions");
        this.commandSink = Objects.requireNonNull(commandSink, "commandSink");
    }

    public void submitScan() {
        boolean submitted = persistenceQueue.submit(() -> {
            List<ExecutionId> ids = repository.listExecutionIds();
            return () -> ids.forEach(this::submitLoad);
        });
        if (!submitted) {
            diagnosticSink.accept(diagnostics.persistenceQueueRejected(ExecutionId.of("echo_story_engine:recovery"),
                    clock.now()));
        }
    }

    private void submitLoad(ExecutionId executionId) {
        boolean submitted = persistenceQueue.submit(() -> {
            ExecutionLoadResult loaded = repository.load(executionId);
            return () -> afterLoad(loaded);
        });
        if (!submitted) {
            diagnosticSink.accept(diagnostics.persistenceQueueRejected(executionId, clock.now()));
        }
    }

    private void afterLoad(ExecutionLoadResult loaded) {
        loaded.diagnostics().forEach(diagnosticSink::accept);
        if (loaded.checkpoint().isEmpty()) {
            return;
        }
        var state = loaded.checkpoint().orElseThrow().state();
        if (state.phase() == StoryExecutionPhase.FINISHED || state.phase() == StoryExecutionPhase.FAILED) {
            MinecraftForge.EVENT_BUS.post(new EchoStoryExecutionTerminalEvent(
                    server, state.executionId(), state.phase()));
            return;
        }
        RecoveryResult recovered = recoveryPlanner.recover(
                loaded.checkpoint().orElseThrow(),
                storyId -> catalogState.current().find(storyId).map(CompiledStoryCatalogEntry::definition),
                clock.now());
        recovered.diagnostics().forEach(diagnosticSink::accept);
        if (recovered.status() == RecoveryStatus.RECOVERY_REQUESTED) {
            dispatch(recovered);
        }
    }

    private void dispatch(RecoveryResult recovered) {
        catalogState.current().find(recovered.state().storyId()).ifPresent(entry -> {
            activeDefinitions.remember(recovered.state().executionId(), entry);
            recovered.commands().forEach(command -> commandSink.recover(recovered.state(), command));
        });
    }
}
