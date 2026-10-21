package io.github.kxyr0.echostoryengine.story.forge.host;

import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.execution.ExternalCommandSink;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ExtensionCatalog;
import io.github.kxyr0.echostoryengine.story.forge.extension.dispatch.ExtensionCompletionQueue;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.host.execution.ActiveStoryDefinitions;
import io.github.kxyr0.echostoryengine.story.forge.host.execution.HostRuntimeFlow;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.ExecutionReadModel;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostHealth;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostSnapshot;
import io.github.kxyr0.echostoryengine.story.forge.host.recovery.StoryRecoveryDispatch;
import io.github.kxyr0.echostoryengine.story.forge.persistence.PersistenceWorkQueue;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

/**
 * One server-scoped host for resource catalog, persistence queue and runtime requests.
 */
public final class ForgeServerHost {
    private static final int DRAIN_BUDGET = 32;
    private static final int EXECUTION_HISTORY_LIMIT = 128;
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);

    private final MinecraftServer server;
    private final StoryCatalogState catalogState;
    private final PersistenceWorkQueue persistenceQueue;
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;
    private final ExtensionCompletionQueue completionQueue;
    private final ExtensionCatalog extensionCatalog;
    private final HostRuntimeFlow runtimeFlow;
    private final StoryRecoveryDispatch recoveryDispatch;
    private final ActiveStoryDefinitions activeDefinitions = new ActiveStoryDefinitions();
    private final ExecutionReadModel executionReadModel = new ExecutionReadModel(EXECUTION_HISTORY_LIMIT);
    private ForgeServerHostState state = ForgeServerHostState.NOT_STARTED;

    public ForgeServerHost(
            MinecraftServer server,
            StoryCatalogState catalogState,
            ExecutionStateRepository repository,
            PersistenceWorkQueue persistenceQueue,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics,
            ExternalCommandSink commandSink) {
        this(server, catalogState, repository, persistenceQueue, diagnosticSink, clock, diagnostics, commandSink,
                new ExtensionCompletionQueue(0), ExtensionCatalog.empty());
    }

    public ForgeServerHost(
            MinecraftServer server,
            StoryCatalogState catalogState,
            ExecutionStateRepository repository,
            PersistenceWorkQueue persistenceQueue,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics,
            ExternalCommandSink commandSink,
            ExtensionCompletionQueue completionQueue,
            ExtensionCatalog extensionCatalog) {
        this.server = Objects.requireNonNull(server, "server");
        this.catalogState = Objects.requireNonNull(catalogState, "catalogState");
        this.persistenceQueue = Objects.requireNonNull(persistenceQueue, "persistenceQueue");
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.completionQueue = Objects.requireNonNull(completionQueue, "completionQueue");
        this.extensionCatalog = Objects.requireNonNull(extensionCatalog, "extensionCatalog");
        this.runtimeFlow = new HostRuntimeFlow(server, repository, persistenceQueue, diagnosticSink, clock, diagnostics,
                commandSink, activeDefinitions, executionReadModel);
        this.recoveryDispatch = new StoryRecoveryDispatch(server, repository, persistenceQueue, diagnosticSink, clock,
                diagnostics, catalogState, activeDefinitions, commandSink);
    }

    public void start() {
        state = ForgeServerHostState.STARTING;
        diagnosticSink.accept(diagnostics.hostStarted(clock.now()));
        state = ForgeServerHostState.ACTIVE;
        recoveryDispatch.submitScan();
    }

    public void stop() {
        state = ForgeServerHostState.STOPPING;
        completionQueue.shutdown();
        persistenceQueue.shutdown(SHUTDOWN_TIMEOUT);
        persistenceQueue.drain(Integer.MAX_VALUE);
        extensionCatalog.close();
        activeDefinitions.clear();
        diagnosticSink.accept(diagnostics.hostStopped(clock.now()));
        state = ForgeServerHostState.STOPPED;
    }

    public void tick() {
        if (state == ForgeServerHostState.ACTIVE) {
            completionQueue.drain(DRAIN_BUDGET, this::applyExtensionCompletion);
            persistenceQueue.drain(DRAIN_BUDGET);
        }
    }

    public void startExecution(StoryId storyId, ExecutionId executionId) {
        StoryRuntimeInput input = StoryRuntimeInput.start(storyId, executionId);
        entryFor(input).ifPresentOrElse(
                entry -> runtimeFlow.submit(input, entry),
                () -> diagnosticSink.accept(diagnostics.missingStory(storyId, executionId, clock.now())));
    }

    public void startDurable(StoryId storyId, ExecutionId executionId,
            java.util.function.Consumer<Boolean> committed) {
        StoryRuntimeInput input = StoryRuntimeInput.start(storyId, executionId);
        entryFor(input).ifPresentOrElse(entry -> runtimeFlow.submit(input, entry, committed),
                () -> committed.accept(false));
    }

    public void completeAction(
            StoryId storyId,
            ExecutionId executionId,
            ActionExecutionId actionId,
            ExternalActionResultCategory resultCategory) {
        StoryRuntimeInput input = StoryRuntimeInput.externalActionCompleted(
                storyId,
                executionId,
                actionId,
                resultCategory);
        entryFor(input).ifPresentOrElse(
                entry -> runtimeFlow.submit(input, entry),
                () -> diagnosticSink.accept(diagnostics.missingStory(storyId, executionId, clock.now())));
    }

    public boolean owns(MinecraftServer candidate) {
        return server == candidate;
    }

    public HostHealth health() {
        return new HostHealth(
                state,
                catalogState.current().revision(),
                catalogState.current().storyCount(),
                catalogState.latestReload(),
                activeDefinitions.size(),
                executionReadModel.waitingCount(),
                persistenceQueue.pendingCount(),
                persistenceQueue.capacity(),
                completionQueue.pendingCount(),
                completionQueue.capacity());
    }

    public HostSnapshot snapshot() {
        return new HostSnapshot(health(), executionReadModel.summaries());
    }

    public ExtensionCatalog extensionCatalog() {
        return extensionCatalog;
    }

    private Optional<CompiledStoryCatalogEntry> entryFor(StoryRuntimeInput input) {
        return activeDefinitions.find(input.executionId()).or(() -> catalogState.current().find(input.storyId()));
    }

    private void applyExtensionCompletion(ActionCompletion completion) {
        StoryRuntimeInput input = ExtensionCompletionInputs.from(completion);
        entryFor(input).ifPresentOrElse(
                entry -> runtimeFlow.submit(input, entry),
                () -> diagnosticSink.accept(diagnostics.missingStory(
                        completion.storyId(),
                        completion.executionId(),
                        clock.now())));
    }

    void workerFailed(RuntimeException exception) {
        StoryDiagnostic diagnostic = diagnostics.persistenceWorkerFailed(
                exception.getClass().getSimpleName(),
                clock.now());
        diagnosticSink.accept(diagnostic);
        state = ForgeServerHostState.FAILED;
    }
}
