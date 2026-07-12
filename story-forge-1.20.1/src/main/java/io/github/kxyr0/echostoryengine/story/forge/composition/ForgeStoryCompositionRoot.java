package io.github.kxyr0.echostoryengine.story.forge.composition;

import io.github.kxyr0.echostoryengine.story.api.StoryEngineApi;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.application.StoryRuntimeUseCase;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeDiagnosticSink;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeStoryClock;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.VolatileExecutionStateRepository;
import java.util.Map;
import java.util.Set;

/**
 * Forge composition root for the minimal runtime.
 */
public final class ForgeStoryCompositionRoot {
    private static final int DIAGNOSTIC_CAPACITY = 128;
    private static final int TRANSITION_BUDGET = 16;

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
                new RuntimeTransitionPolicy(
                        helloStory(),
                        Set.of(NamespacedId.of("echo_story_engine:hello_action")),
                        TRANSITION_BUDGET));
        return new ForgeStoryCompositionRoot(storyEngine, diagnosticBuffer);
    }

    public StoryEngineApi storyEngine() {
        return storyEngine;
    }

    public InMemoryDiagnosticBuffer diagnosticBuffer() {
        return diagnosticBuffer;
    }

    private static StoryDefinition helloStory() {
        NodeId start = NodeId.root();
        NodeId setVariable = NodeId.of("set_hello_flag");
        NodeId condition = NodeId.of("check_hello_flag");
        NodeId action = NodeId.of("request_hello_action");
        NodeId finished = NodeId.of("finished");
        return new StoryDefinition(
                StoryId.of("echo_story_engine:hello_story"),
                start,
                Map.of(
                        start, StoryNode.start(start, setVariable),
                        setVariable, StoryNode.setVariable(setVariable, "hello.flag", "ready", condition),
                        condition, StoryNode.condition(condition, "hello.flag", "ready", action),
                        action, StoryNode.externalAction(
                                action,
                                NamespacedId.of("echo_story_engine:hello_action"),
                                Map.of(),
                                finished),
                        finished, StoryNode.finish(finished)));
    }
}
