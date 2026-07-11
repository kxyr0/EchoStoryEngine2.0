package io.github.kxyr0.echostoryengine.story.forge.composition;

import io.github.kxyr0.echostoryengine.story.api.StoryEngineApi;
import io.github.kxyr0.echostoryengine.story.core.application.StoryRuntimeUseCase;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeDiagnosticSink;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeStoryClock;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.VolatileExecutionStateRepository;

/**
 * Forge composition root for the minimal runtime.
 */
public final class ForgeStoryCompositionRoot {
    private static final int DIAGNOSTIC_CAPACITY = 128;

    private final StoryEngineApi storyEngine;
    private final InMemoryDiagnosticBuffer diagnosticBuffer;

    private ForgeStoryCompositionRoot(StoryEngineApi storyEngine, InMemoryDiagnosticBuffer diagnosticBuffer) {
        this.storyEngine = storyEngine;
        this.diagnosticBuffer = diagnosticBuffer;
    }

    public static ForgeStoryCompositionRoot create() {
        InMemoryDiagnosticBuffer diagnosticBuffer = new InMemoryDiagnosticBuffer(DIAGNOSTIC_CAPACITY);
        DiagnosticSink forgeSink = new ForgeDiagnosticSink();
        DiagnosticSink combinedSink = diagnostic -> {
            diagnosticBuffer.accept(diagnostic);
            forgeSink.accept(diagnostic);
        };
        StoryEngineApi storyEngine = new StoryRuntimeUseCase(
                new VolatileExecutionStateRepository(),
                combinedSink,
                new ForgeStoryClock(),
                new RuntimeTransitionPolicy());
        return new ForgeStoryCompositionRoot(storyEngine, diagnosticBuffer);
    }

    public StoryEngineApi storyEngine() {
        return storyEngine;
    }

    public InMemoryDiagnosticBuffer diagnosticBuffer() {
        return diagnosticBuffer;
    }
}
