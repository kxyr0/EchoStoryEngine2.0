package io.github.kxyr0.echoaudio.forge.dialogue;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.*;
import io.github.kxyr0.echodialogue.api.observation.*;
import io.github.kxyr0.echodialogue.api.service.*;
import io.github.kxyr0.echodialogue.api.session.*;
import io.github.kxyr0.echodialogue.api.wait.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioDialogueSessionTest {
    private final DialogueSessionId id = new DialogueSessionId("audio-test");
    private DialogueExternalWait waiting;
    private int revision = 3;
    private int resumes;
    private int cancellations;
    private final AtomicInteger stops = new AtomicInteger();
    private DialogueTerminalObserver observer;
    private final DialogueService service = (DialogueService) Proxy.newProxyInstance(
            DialogueService.class.getClassLoader(), new Class<?>[]{DialogueService.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "query": return new DialogueQueryResult(DialogueOperationStatus.ACCEPTED,
                            Optional.of(view()), List.of());
                    case "resumeExternal": resumes++; waiting = null; revision++; return null;
                    case "cancel": cancellations++; waiting = null; return null;
                    case "observeTerminal": observer = (DialogueTerminalObserver) args[1];
                        return (DialogueObservationHandle) () -> observer = null;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });

    private DialogueSessionView view() {
        return new DialogueSessionView(id, DialogueId.parse("jdh:test"), new AudienceId("player"),
                DialogueSessionStatus.WAITING, revision, 0, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.ofNullable(waiting));
    }
    private DialogueExternalWait waitFor(String operation) {
        var cue = new AudioCue(operation, "jdh:knock", "", "", new AudioOptions(1, 1, 0, 0, 0));
        waiting = new DialogueExternalWait("echo_audio:cue", "knock", DialogueCommandPayload.encode(cue.payload()),
                new DialogueNodeId("intro"), 1);
        return waiting;
    }
    private AudioDialogueSession session() {
        var session = new AudioDialogueSession(service, id, stops::incrementAndGet);
        session.observe(); return session;
    }
    @Test void fireContinuesImmediatelyAndLateFailureDoesNotCancelNextLine() {
        var wait = waitFor("Play"); var session = session();
        AtomicReference<Consumer<String>> completion = new AtomicReference<>();
        session.dispatch(wait, (cue, done) -> completion.set(done));
        assertEquals(1, resumes);
        completion.get().accept("AUDIO_ASSET_NOT_FOUND");
        assertEquals(0, cancellations);
    }
    @Test void awaitCompletesOnceAndDuplicateDispatchDoesNotRestartAudio() {
        var wait = waitFor("PlayAndWait"); var session = session();
        var starts = new AtomicInteger();
        AtomicReference<Consumer<String>> completion = new AtomicReference<>();
        session.dispatch(wait, (cue, done) -> { starts.incrementAndGet(); completion.set(done); });
        session.dispatch(wait, (cue, done) -> starts.incrementAndGet());
        assertEquals(1, starts.get()); assertEquals(0, resumes);
        completion.get().accept(""); completion.get().accept("");
        assertEquals(1, resumes);
    }
    @Test void awaitFailureCancelsDialogue() {
        var wait = waitFor("PlayAndWait"); var session = session();
        session.dispatch(wait, (cue, done) -> done.accept("AUDIO_ASSET_NOT_FOUND"));
        assertEquals(1, cancellations); assertEquals(0, resumes);
    }
    @Test void terminalStopsOwnerOnceAndIgnoresLateCompletion() {
        var wait = waitFor("PlayAndWait"); var session = session();
        AtomicReference<Consumer<String>> completion = new AtomicReference<>();
        session.dispatch(wait, (cue, done) -> completion.set(done));
        observer.onTerminal(new DialogueTerminalEvent(view()));
        completion.get().accept(""); session.close();
        assertEquals(1, stops.get()); assertEquals(0, resumes);
    }
    @Test void oldCompletionCannotResumeLaterVisitToSameCommand() {
        var wait = waitFor("PlayAndWait"); var session = session();
        AtomicReference<Consumer<String>> first = new AtomicReference<>();
        session.dispatch(wait, (cue, done) -> first.set(done));
        first.get().accept("");
        waiting = wait;
        session.dispatch(wait, (cue, done) -> { });
        first.get().accept("");
        assertEquals(1, resumes); assertNotNull(waiting);
    }
    @Test void sameCommandOnLaterNodeVisitPlaysAgain() {
        var wait = waitFor("Play"); var session = session(); var starts = new AtomicInteger();
        session.dispatch(wait, (cue, done) -> starts.incrementAndGet());
        waiting = wait;
        session.dispatch(wait, (cue, done) -> starts.incrementAndGet());
        assertEquals(2, starts.get()); assertEquals(2, resumes);
    }
}
