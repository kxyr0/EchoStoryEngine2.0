package io.github.kxyr0.echostoryengine.story.forge.composition;

import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.echoscript.environment.EchoScriptEnvironmentSourceCompiler;
import io.github.kxyr0.echostoryengine.story.echoscript.EchoScriptStorySourceCompiler;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentResourceCandidateCompiler;
import io.github.kxyr0.echostoryengine.story.forge.externalcontent.ExternalContentLifecycle;
import io.github.kxyr0.echostoryengine.story.forge.host.ForgeStoryRuntimeCoordinator;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeDiagnosticSink;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeStoryClock;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceCandidateCompiler;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Собирает Forge adapters, не исполняя story logic в constructor.
 */
public final class ForgeStoryCompositionRoot {
    private static final int DIAGNOSTIC_CAPACITY = 128;

    private final ForgeStoryRuntimeCoordinator coordinator;
    private final InMemoryDiagnosticBuffer diagnosticBuffer;
    private final ExternalContentLifecycle externalContent;

    private ForgeStoryCompositionRoot(
            ForgeStoryRuntimeCoordinator coordinator,
            InMemoryDiagnosticBuffer diagnosticBuffer,
            ExternalContentLifecycle externalContent) {
        this.coordinator = coordinator;
        this.diagnosticBuffer = diagnosticBuffer;
        this.externalContent = externalContent;
    }

    public static ForgeStoryCompositionRoot create() {
        InMemoryDiagnosticBuffer diagnosticBuffer = new InMemoryDiagnosticBuffer(DIAGNOSTIC_CAPACITY);
        DiagnosticSink forgeSink = new ForgeDiagnosticSink();
        DiagnosticSink combinedSink = diagnostic -> {
            diagnosticBuffer.accept(diagnostic);
            forgeSink.accept(diagnostic);
        };
        ForgeStoryClock clock = new ForgeStoryClock();
        ForgeRuntimeDiagnostics diagnostics = new ForgeRuntimeDiagnostics();
        StoryCatalogState catalogState = new StoryCatalogState(clock.now());
        EnvironmentCatalogState environmentCatalogState = new EnvironmentCatalogState(clock.now());
        StoryResourceCandidateCompiler compiler = new StoryResourceCandidateCompiler(
                new EchoScriptStorySourceCompiler(),
                diagnostics);
        EnvironmentResourceCandidateCompiler environmentCompiler = new EnvironmentResourceCandidateCompiler(
                new EchoScriptEnvironmentSourceCompiler());
        ForgeStoryRuntimeCoordinator coordinator = new ForgeStoryRuntimeCoordinator(
                catalogState,
                environmentCatalogState,
                diagnosticBuffer,
                combinedSink,
                clock,
                diagnostics,
                compiler,
                environmentCompiler);
        return new ForgeStoryCompositionRoot(coordinator, diagnosticBuffer, new ExternalContentLifecycle());
    }

    public void register(IEventBus modEventBus, IEventBus forgeEventBus) {
        externalContent.register(modEventBus, forgeEventBus);
        coordinator.register(modEventBus, forgeEventBus);
    }

    public ForgeStoryRuntimeCoordinator coordinator() {
        return coordinator;
    }

    public InMemoryDiagnosticBuffer diagnosticBuffer() {
        return diagnosticBuffer;
    }
}
