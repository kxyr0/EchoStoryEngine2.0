package io.github.kxyr0.echoobservation.core;

import io.github.kxyr0.echoobservation.api.action.ObservationActionExecutor;
import io.github.kxyr0.echoobservation.api.action.ObservationActionRegistry;
import io.github.kxyr0.echoobservation.core.focus.ObservationFocusSessions;
import io.github.kxyr0.echoobservation.core.progress.ObservationCompletionFlow;
import io.github.kxyr0.echoobservation.core.progress.ObservationCompletionResult;
import io.github.kxyr0.echoobservation.core.progress.ObservationPlayerProgress;
import io.github.kxyr0.echoobservation.core.progress.ObservationProgressBook;
import io.github.kxyr0.echoobservation.echoscript.ObservationResourceCompiler;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationRuntimeTest {
    @Test
    void focusUsesPinnedServerTicksAndBoundedHeartbeat() {
        var definition = definition("Focus(1.0);");
        UUID player = UUID.randomUUID();
        ObservationFocusSessions sessions = new ObservationFocusSessions();
        sessions.start(player, definition, 100L);
        assertTrue(sessions.heartbeat(player, definition.id(), definition.revision(), 104L));
        assertTrue(sessions.heartbeat(player, definition.id(), definition.revision(), 109L));
        assertTrue(sessions.heartbeat(player, definition.id(), definition.revision(), 114L));
        assertTrue(sessions.completable(119L).isEmpty());
        assertTrue(sessions.heartbeat(player, definition.id(), definition.revision(), 119L));
        assertEquals(1, sessions.completable(120L).size());
    }

    @Test
    void staleOrMissingHeartbeatCannotComplete() {
        var definition = definition("Focus(1.0);");
        UUID player = UUID.randomUUID();
        ObservationFocusSessions sessions = new ObservationFocusSessions();
        sessions.start(player, definition, 10L);
        assertTrue(sessions.completable(30L).isEmpty());
        assertFalse(sessions.session(player).isPresent());
    }

    @Test
    void completionActionsUseStableIdsAndExecuteOnce() {
        var definition = definition("OnComplete() { Story.Start(\"jdh:done\"); }");
        AtomicInteger executions = new AtomicInteger();
        ObservationActionRegistry registry = new ObservationActionRegistry();
        registry.register(executor(executions));
        ObservationPlayerProgress progress = new ObservationPlayerProgress();
        ObservationCompletionFlow flow = new ObservationCompletionFlow(registry);
        UUID player = UUID.randomUUID();

        assertEquals(ObservationCompletionResult.Status.COMPLETED,
                flow.complete(player, definition, progress).status());
        assertEquals(ObservationCompletionResult.Status.ALREADY_COMPLETED,
                flow.complete(player, definition, progress).status());
        assertEquals(1, executions.get());
    }

    @Test
    void developerResetAllowsCompletionAndActionsToRunAgain() {
        var definition = definition("OnComplete() { Story.Start(\"jdh:done\"); }");
        AtomicInteger executions = new AtomicInteger();
        ObservationActionRegistry registry = new ObservationActionRegistry();
        registry.register(executor(executions));
        ObservationPlayerProgress progress = new ObservationPlayerProgress();
        ObservationCompletionFlow flow = new ObservationCompletionFlow(registry);
        UUID player = UUID.randomUUID();

        flow.complete(player, definition, progress);
        assertTrue(progress.resetCompletion(definition.id()));
        assertFalse(progress.completed(definition.id()));
        flow.complete(player, definition, progress);

        assertEquals(2, executions.get());
    }

    @Test
    void multiplayerProgressIsIndependent() {
        var definition = definition("Focus(1.0);");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ObservationProgressBook book = new ObservationProgressBook();
        book.player(first).activate(definition.id().resource());
        book.player(first).complete(definition.id(),
                io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId.deterministic(
                        first, definition.id()));

        assertTrue(book.player(first).active(definition.id().resource()));
        assertTrue(book.player(first).completed(definition.id()));
        assertFalse(book.player(second).active(definition.id().resource()));
        assertFalse(book.player(second).completed(definition.id()));
    }

    @Test
    void formerLocalIdMigratesCompletionWithoutChangingItsDurableId() {
        UUID player = UUID.randomUUID();
        var current = definition("Formerly(\"old_a\");");
        var former = io.github.kxyr0.echoobservation.api.identity.ObservationId.parse(
                current.id().resource() + "#old_a");
        var completion = io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId.deterministic(
                player, former);
        ObservationPlayerProgress progress = new ObservationPlayerProgress();
        progress.complete(former, completion);

        assertTrue(progress.migrate(former, current.id()));
        assertFalse(progress.completed(former));
        assertEquals(completion, progress.completion(current.id()).orElseThrow());
    }

    private ObservationActionExecutor executor(AtomicInteger executions) {
        return new ObservationActionExecutor() {
            public String target() { return "Story.Start"; }
            public boolean execute(io.github.kxyr0.echoobservation.api.action.ObservationActionContext context,
                    io.github.kxyr0.echoobservation.api.action.ObservationScriptAction action) {
                executions.incrementAndGet();
                return true;
            }
        };
    }

    private io.github.kxyr0.echoobservation.api.model.ObservationDefinition definition(String body) {
        String source = "class X : ObservationResource { Observation A() { " + body + " } }";
        return new ObservationResourceCompiler().compile(
                "jdh:echo_observation/observations/test.es", source)
                .resource().orElseThrow().observations().get(0);
    }
}
