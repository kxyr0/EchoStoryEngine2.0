package io.github.kxyr0.echodialogue.core.host;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Server-neutral one-active-session policy for D1.
 */
public final class ActiveDialogueSessions {
    private final DialogueSessionEngine engine;
    private final Map<AudienceId, RunningDialogue> sessions = new HashMap<>();

    public ActiveDialogueSessions(DialogueSessionEngine engine) {
        this.engine = engine;
    }

    public DialogueTransition start(AudienceId audienceId, DialogueSessionId sessionId, CompiledDialogue dialogue) {
        Optional<RunningDialogue> current = activeSession(audienceId);
        if (current.isPresent()) {
            return secondSessionRejected(current.get().state());
        }
        DialogueTransition transition = engine.start(dialogue, sessionId, audienceId);
        install(dialogue, transition.state());
        return transition;
    }

    public DialogueTransition previewStart(
            AudienceId audienceId,
            DialogueSessionId sessionId,
            CompiledDialogue dialogue
    ) {
        Optional<RunningDialogue> current = activeSession(audienceId);
        return current.map(running -> secondSessionRejected(running.state()))
                .orElseGet(() -> engine.start(dialogue, sessionId, audienceId));
    }

    public Optional<DialogueSessionState> stateFor(AudienceId audienceId) {
        return Optional.ofNullable(sessions.get(audienceId)).map(RunningDialogue::state);
    }

    public Optional<DialogueSessionState> stateBySessionId(DialogueSessionId sessionId) {
        return sessions.values().stream()
                .map(RunningDialogue::state)
                .filter(state -> state.sessionId().equals(sessionId))
                .findFirst();
    }

    public int activeCount() {
        return (int) sessions.values().stream()
                .map(RunningDialogue::state)
                .filter(state -> state.phase() == DialogueSessionPhase.ACTIVE
                        || state.phase() == DialogueSessionPhase.WAITING)
                .count();
    }

    public Set<AudienceId> activeAudienceIds() {
        return sessions.values().stream()
                .map(RunningDialogue::state)
                .filter(state -> state.phase() == DialogueSessionPhase.ACTIVE
                        || state.phase() == DialogueSessionPhase.WAITING)
                .map(DialogueSessionState::audienceId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void clear() {
        sessions.clear();
    }

    public void install(CompiledDialogue dialogue, DialogueSessionState state) {
        sessions.put(state.audienceId(), new RunningDialogue(dialogue, state));
    }

    public Optional<CompiledDialogue> dialogueFor(AudienceId audienceId) {
        return Optional.ofNullable(sessions.get(audienceId)).map(RunningDialogue::dialogue);
    }

    public Optional<CompiledDialogue> dialogueBySessionId(DialogueSessionId sessionId) {
        return sessions.values().stream()
                .filter(running -> running.state().sessionId().equals(sessionId))
                .map(RunningDialogue::dialogue)
                .findFirst();
    }

    public Optional<DialogueTransition> preview(AudienceId audienceId, DialogueInput input) {
        RunningDialogue running = sessions.get(audienceId);
        if (running == null) {
            return Optional.empty();
        }
        return Optional.of(engine.apply(running.dialogue(), running.state(), input));
    }

    public Optional<DialogueTransition> previewWaitExternal(
            AudienceId audienceId,
            DialogueSessionId sessionId,
            int expectedRevision,
            DialogueExternalWait wait
    ) {
        RunningDialogue running = sessions.get(audienceId);
        if (running == null || !running.state().sessionId().equals(sessionId)) {
            return Optional.empty();
        }
        return Optional.of(engine.waitExternal(running.dialogue(), running.state(), expectedRevision, wait));
    }

    public Optional<DialogueTransition> previewResumeExternal(
            AudienceId audienceId,
            DialogueSessionId sessionId,
            int expectedRevision,
            String waitKey
    ) {
        RunningDialogue running = sessions.get(audienceId);
        if (running == null || !running.state().sessionId().equals(sessionId)) {
            return Optional.empty();
        }
        return Optional.of(engine.resumeExternal(running.dialogue(), running.state(), expectedRevision, waitKey));
    }

    public Optional<DialogueTransition> apply(AudienceId audienceId, DialogueInput input) {
        Optional<DialogueTransition> transition = preview(audienceId, input);
        transition.ifPresent(value -> dialogueFor(audienceId).ifPresent(dialogue -> install(dialogue, value.state())));
        return transition;
    }

    private Optional<RunningDialogue> activeSession(AudienceId audienceId) {
        RunningDialogue running = sessions.get(audienceId);
        if (running == null || terminal(running.state().phase())) {
            return Optional.empty();
        }
        return Optional.of(running);
    }

    private boolean terminal(DialogueSessionPhase phase) {
        return phase == DialogueSessionPhase.COMPLETED
                || phase == DialogueSessionPhase.CANCELLED
                || phase == DialogueSessionPhase.FAILED;
    }

    private DialogueTransition secondSessionRejected(DialogueSessionState state) {
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-CORE-005",
                DiagnosticCategory.SECOND_SESSION_REJECTED, "У игрока уже есть активная сессия диалога.");
        return DialogueTransition.of(state, DialogueTransitionStatus.REJECTED, Optional.empty(), diagnostic);
    }

    private record RunningDialogue(CompiledDialogue dialogue, DialogueSessionState state) {
    }
}
