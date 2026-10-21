package io.github.kxyr0.echostoryengine.story.forge.host.execution;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.execution.DurableStoryRuntime;
import io.github.kxyr0.echostoryengine.story.forge.execution.ExternalCommandSink;
import io.github.kxyr0.echostoryengine.story.forge.execution.StoryRuntimeDispatch;
import io.github.kxyr0.echostoryengine.story.forge.execution.StoryRuntimeSavePlan;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.ExecutionReadModel;
import io.github.kxyr0.echostoryengine.story.forge.persistence.PersistenceWorkQueue;
import io.github.kxyr0.echostoryengine.story.extension.forge.lifecycle.EchoStoryExecutionTerminalEvent;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Applies load-transition-save ordering and exposes commands only after successful persistence.
 */
public final class HostRuntimeFlow {
    private final MinecraftServer server;
    private final ExecutionStateRepository repository;
    private final PersistenceWorkQueue persistenceQueue;
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;
    private final ExternalCommandSink commandSink;
    private final ActiveStoryDefinitions activeDefinitions;
    private final ExecutionReadModel executionReadModel;
    private final DurableStoryRuntime runtime;

    public HostRuntimeFlow(
            MinecraftServer server,
            ExecutionStateRepository repository,
            PersistenceWorkQueue persistenceQueue,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics,
            ExternalCommandSink commandSink,
            ActiveStoryDefinitions activeDefinitions,
            ExecutionReadModel executionReadModel) {
        this.server = Objects.requireNonNull(server, "server");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.persistenceQueue = Objects.requireNonNull(persistenceQueue, "persistenceQueue");
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.commandSink = Objects.requireNonNull(commandSink, "commandSink");
        this.activeDefinitions = Objects.requireNonNull(activeDefinitions, "activeDefinitions");
        this.executionReadModel = Objects.requireNonNull(executionReadModel, "executionReadModel");
        this.runtime = new DurableStoryRuntime(diagnostics);
    }

    public void submit(StoryRuntimeInput input, CompiledStoryCatalogEntry entry) {
        submit(input, entry, ignored -> { });
    }

    public void submit(StoryRuntimeInput input, CompiledStoryCatalogEntry entry,
            java.util.function.Consumer<Boolean> committed) {
        boolean submitted = persistenceQueue.submit(() -> {
            ExecutionLoadResult loaded = repository.load(input.executionId());
            return () -> afterLoad(input, entry, loaded, committed);
        });
        if (!submitted) {
            committed.accept(false);
            diagnosticSink.accept(diagnostics.persistenceQueueRejected(input.executionId(), clock.now()));
        }
    }

    private void afterLoad(
            StoryRuntimeInput input,
            CompiledStoryCatalogEntry entry,
            ExecutionLoadResult loaded, java.util.function.Consumer<Boolean> committed) {
        boolean existing = loaded.checkpoint().filter(checkpoint -> checkpoint.state().storyId().equals(input.storyId())
                && checkpoint.state().phase() != StoryExecutionPhase.READY).isPresent();
        if (existing) {
            committed.accept(true);
        }
        StoryRuntimeSavePlan plan = runtime.afterLoad(input, entry, loaded, clock.now());
        if (!plan.requiresSave()) {
            plan.diagnostics().forEach(diagnosticSink::accept);
            if (!existing) { committed.accept(false); }
            return;
        }
        submitSave(plan, existing ? ignored -> { } : committed);
    }

    private void submitSave(StoryRuntimeSavePlan plan, java.util.function.Consumer<Boolean> committed) {
        boolean submitted = persistenceQueue.submit(() -> {
            ExecutionSaveResult saved = repository.save(plan.checkpoint().orElseThrow(), plan.expectedRevision());
            return () -> afterSave(plan, saved, committed);
        });
        if (!submitted) {
            committed.accept(false);
            diagnosticSink.accept(diagnostics.persistenceQueueRejected(plan.input().executionId(), clock.now()));
        }
    }

    private void afterSave(StoryRuntimeSavePlan plan, ExecutionSaveResult saved,
            java.util.function.Consumer<Boolean> committed) {
        StoryRuntimeDispatch dispatch = runtime.afterSave(plan, saved, clock.now());
        dispatch.diagnostics().forEach(diagnosticSink::accept);
        if (!dispatch.saved()) {
            committed.accept(false);
            return;
        }
        committed.accept(true);
        executionReadModel.update(plan, dispatch);
        updateActive(dispatch);
        if (runtime.terminal(dispatch.state())) {
            MinecraftForge.EVENT_BUS.post(
                    new EchoStoryExecutionTerminalEvent(
                            server, dispatch.state().executionId(), dispatch.state().phase()));
        }
        dispatch.commands().forEach(command -> commandSink.dispatch(dispatch.state(), command));
    }

    private void updateActive(StoryRuntimeDispatch dispatch) {
        if (runtime.terminal(dispatch.state()) || dispatch.state().phase() == StoryExecutionPhase.READY) {
            activeDefinitions.remove(dispatch.state().executionId());
        } else {
            activeDefinitions.remember(dispatch.state().executionId(), dispatch.catalogEntry());
        }
    }
}
