package io.github.kxyr0.echostoryengine.story.forge.host;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticsCommand;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentResourceCandidateCompiler;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionLookupEvent;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ExtensionCatalog;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ForgeExtensionCatalogBuilder;
import io.github.kxyr0.echostoryengine.story.forge.extension.dispatch.ExtensionCompletionQueue;
import io.github.kxyr0.echostoryengine.story.forge.extension.lifecycle.ForgeExtensionEventPublisher;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostSnapshot;
import io.github.kxyr0.echostoryengine.story.forge.persistence.BoundedPersistenceWorkQueue;
import io.github.kxyr0.echostoryengine.story.forge.persistence.ForgePersistenceRoot;
import io.github.kxyr0.echostoryengine.story.forge.persistence.PersistenceWorkQueue;
import io.github.kxyr0.echostoryengine.story.forge.resource.ForgeStoryResourceReloadListener;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceCandidateCompiler;
import io.github.kxyr0.echostoryengine.story.persistence.file.FileExecutionStateRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Wires Forge events to the current server-scoped host.
 */
public final class ForgeStoryRuntimeCoordinator {
    private static final int PERSISTENCE_QUEUE_CAPACITY = 64, EXTENSION_COMPLETION_CAPACITY = 64;

    private final StoryCatalogState catalogState;
    private final EnvironmentCatalogState environmentCatalogState;
    private final InMemoryDiagnosticBuffer diagnosticBuffer;
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;
    private final StoryResourceCandidateCompiler compiler;
    private final EnvironmentResourceCandidateCompiler environmentCompiler;
    private final ForgeCommandSinkWiring commandSinkWiring;
    private ForgeServerHost activeHost;
    private MinecraftServer server;

    public ForgeStoryRuntimeCoordinator(
            StoryCatalogState catalogState,
            EnvironmentCatalogState environmentCatalogState,
            InMemoryDiagnosticBuffer diagnosticBuffer,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics,
            StoryResourceCandidateCompiler compiler,
            EnvironmentResourceCandidateCompiler environmentCompiler) {
        this.catalogState = Objects.requireNonNull(catalogState, "catalogState");
        this.environmentCatalogState = Objects.requireNonNull(environmentCatalogState, "environmentCatalogState");
        this.diagnosticBuffer = Objects.requireNonNull(diagnosticBuffer, "diagnosticBuffer");
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.environmentCompiler = Objects.requireNonNull(environmentCompiler, "environmentCompiler");
        this.commandSinkWiring = new ForgeCommandSinkWiring(
                environmentCatalogState, diagnosticSink, clock, diagnostics);
    }

    public void register(IEventBus modEventBus, IEventBus forgeEventBus) {
        Objects.requireNonNull(modEventBus, "modEventBus");
        forgeEventBus.addListener(this::onAddReloadListener);
        forgeEventBus.addListener(this::onRegisterCommands);
        forgeEventBus.addListener(this::onServerStarted);
        forgeEventBus.addListener(this::onServerStopping);
        forgeEventBus.addListener(this::onServerStopped);
        forgeEventBus.addListener(this::onServerTick);
        forgeEventBus.addListener(this::onExecutionLookup);
    }

    public Optional<ForgeServerHost> activeHost() { return Optional.ofNullable(activeHost); }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ForgeStoryResourceReloadListener(
                compiler,
                environmentCompiler,
                catalogState,
                environmentCatalogState,
                diagnosticSink,
                clock,
                diagnostics,
                () -> Optional.ofNullable(server)));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ForgeDiagnosticsCommand.register(event.getDispatcher(), new ForgeDiagnosticsCommand.CommandRuntimeView(
                diagnosticBuffer,
                this::snapshot,
                catalogState,
                this::extensionCatalog));
    }

    private void onServerStarted(ServerStartedEvent event) { replaceHost(event.getServer()); }

    private void onServerStopping(ServerStoppingEvent event) {
        if (activeHost != null && activeHost.owns(event.getServer())) {
            activeHost.stop();
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        if (activeHost != null && activeHost.owns(event.getServer())) {
            activeHost = null;
            server = null;
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && activeHost != null && activeHost.owns(event.getServer())) {
            activeHost.tick();
        }
    }

    private void onExecutionLookup(EchoStoryExecutionLookupEvent event) {
        if (activeHost != null && activeHost.owns(event.server())) {
            event.register(new io.github.kxyr0.echostoryengine.story.forge.host.execution
                    .StoryExecutionStarterAdapter(activeHost));
        }
    }

    private Optional<HostSnapshot> snapshot() { return activeHost().map(ForgeServerHost::snapshot); }

    private ExtensionCatalog extensionCatalog() {
        return activeHost().map(ForgeServerHost::extensionCatalog).orElseGet(ExtensionCatalog::empty);
    }

    private void replaceHost(MinecraftServer server) {
        if (activeHost != null) {
            activeHost.stop();
        }
        this.server = server;
        Path root = ForgePersistenceRoot.executionRoot(server);
        PersistenceWorkQueue queue = new BoundedPersistenceWorkQueue(
                PERSISTENCE_QUEUE_CAPACITY,
                "echo-story-engine-persistence",
                this::workerFailed);
        ExtensionCatalog extensions = extensionCatalog(server);
        ExtensionCompletionQueue completions = new ExtensionCompletionQueue(EXTENSION_COMPLETION_CAPACITY);
        activeHost = new ForgeServerHost(
                server,
                catalogState,
                new FileExecutionStateRepository(root, clock),
                queue,
                diagnosticSink,
                clock,
                diagnostics,
                commandSinkWiring.create(
                        server,
                        extensions,
                        completions,
                        new ForgeCommandSinkWiring.StoryLaunchWiring(
                                storyId -> catalogState.current().find(storyId).isPresent(),
                                (storyId, executionId) -> {
                                    if (activeHost == null) {
                                        throw new IllegalStateException("Story host is inactive.");
                                    }
                                    activeHost.startExecution(storyId, executionId);
                                })),
                completions,
                extensions);
        activeHost.start();
    }

    private ExtensionCatalog extensionCatalog(MinecraftServer server) {
        List<ForgeStoryExtensionFactory> factories = new ForgeExtensionEventPublisher(diagnosticSink, clock,
                diagnostics).collect(server);
        ForgeStoryExtensionContext context = new ForgeStoryExtensionContext(server, StoryExtensionApiVersion.current());
        return new ForgeExtensionCatalogBuilder(diagnosticSink, clock, diagnostics).build(factories, context);
    }

    private void workerFailed(RuntimeException exception) {
        if (activeHost != null) {
            activeHost.workerFailed(exception);
            return;
        }
        StoryDiagnostic diagnostic = diagnostics.persistenceWorkerFailed(
                exception.getClass().getSimpleName(),
                clock.now());
        diagnosticSink.accept(diagnostic);
    }
}
