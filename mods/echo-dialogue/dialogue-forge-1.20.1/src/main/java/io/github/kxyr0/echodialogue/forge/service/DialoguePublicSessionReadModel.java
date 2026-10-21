package io.github.kxyr0.echodialogue.forge.service;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.observation.DialogueObservationHandle;
import io.github.kxyr0.echodialogue.api.observation.DialogueTerminalEvent;
import io.github.kxyr0.echodialogue.api.observation.DialogueTerminalObserver;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-thread public read model for adapter-facing session queries.
 */
public final class DialoguePublicSessionReadModel {
    private static final int MAX_OBSERVED_SESSIONS = 1024;
    private static final int MAX_OBSERVERS_PER_SESSION = 16;

    private final ForgeDiagnosticSink diagnosticSink;
    private final Map<DialogueSessionId, DialogueSessionView> pending = new HashMap<>();
    private final Map<DialogueSessionId, DialogueSessionView> retained = new HashMap<>();
    private final Map<DialogueSessionId, List<DialogueTerminalObserver>> observers = new HashMap<>();

    public DialoguePublicSessionReadModel(ForgeDiagnosticSink diagnosticSink) {
        this.diagnosticSink = diagnosticSink;
    }

    public Optional<DialogueSessionView> pendingOrRetained(DialogueSessionId sessionId) {
        return Optional.ofNullable(pending.get(sessionId))
                .or(() -> Optional.ofNullable(retained.get(sessionId)));
    }

    public void pending(DialogueSessionState state) {
        pending.put(state.sessionId(), view(state, new DialogueRepositoryRevision(0)));
    }

    public void saveSucceeded(DialogueCheckpoint checkpoint) {
        pending.remove(checkpoint.sessionId());
        if (terminal(checkpoint.phase())) {
            DialogueSessionView view = view(checkpoint);
            retained.put(checkpoint.sessionId(), view);
            notifyTerminal(view);
        }
    }

    public void saveFailed(DialogueSessionId sessionId) {
        pending.remove(sessionId);
    }

    public void recoveredBlocked(DialogueCheckpoint checkpoint, List<DialogueDiagnostic> diagnostics) {
        String reason = diagnostics.stream()
                .map(DialogueDiagnostic::message)
                .findFirst()
                .orElse("Dialogue recovery blocked.");
        DialogueSessionView view = view(checkpoint, DialogueSessionStatus.BLOCKED, Optional.of(reason));
        retained.put(checkpoint.sessionId(), view);
        pending.remove(checkpoint.sessionId());
    }

    public void resetRecoveredDurableState() {
        retained.clear();
    }

    public DialogueObservationHandle observeTerminal(
            DialogueSessionId sessionId,
            DialogueTerminalObserver observer
    ) {
        Optional<DialogueSessionView> terminal = retainedTerminal(sessionId);
        if (terminal.isPresent()) {
            notifyOne(observer, new DialogueTerminalEvent(terminal.orElseThrow()));
            return () -> { };
        }
        if (!canTrack(sessionId)) {
            diagnosticSink.accept(DialogueDiagnostic.error("EDLG-FORGE-019",
                    DiagnosticCategory.HOST_LIFECYCLE, "Public terminal observer limit exceeded."));
            return () -> { };
        }
        observers.computeIfAbsent(sessionId, ignored -> new ArrayList<>()).add(observer);
        return () -> removeObserver(sessionId, observer);
    }

    public DialogueSessionView view(DialogueSessionState state, DialogueRepositoryRevision repositoryRevision) {
        return new DialogueSessionView(state.sessionId(), state.dialogueId(), state.audienceId(),
                status(state.phase()), state.revision(), repositoryRevision.value(),
                state.outcomeId().map(Object::toString), Optional.empty(), Optional.empty(),
                state.externalWait());
    }

    private Optional<DialogueSessionView> retainedTerminal(DialogueSessionId sessionId) {
        return Optional.ofNullable(retained.get(sessionId)).filter(view -> terminal(view.status()));
    }

    private DialogueSessionView view(DialogueCheckpoint checkpoint) {
        return view(checkpoint, status(checkpoint.phase()), Optional.empty());
    }

    private DialogueSessionView view(
            DialogueCheckpoint checkpoint,
            DialogueSessionStatus status,
            Optional<String> blockedReason
    ) {
        return new DialogueSessionView(checkpoint.sessionId(), checkpoint.dialogueId(), checkpoint.audienceId(),
                status, checkpoint.revision(), checkpoint.repositoryRevision().value(),
                checkpoint.terminalResult(), checkpoint.cancellationReason(), blockedReason,
                checkpoint.externalWait());
    }

    private DialogueSessionStatus status(DialogueSessionPhase phase) {
        return switch (phase) {
            case ACTIVE -> DialogueSessionStatus.ACTIVE;
            case WAITING -> DialogueSessionStatus.WAITING;
            case COMPLETED -> DialogueSessionStatus.COMPLETED;
            case CANCELLED -> DialogueSessionStatus.CANCELLED;
            case FAILED -> DialogueSessionStatus.FAILED;
        };
    }

    private boolean terminal(DialogueSessionPhase phase) {
        return phase == DialogueSessionPhase.COMPLETED
                || phase == DialogueSessionPhase.CANCELLED
                || phase == DialogueSessionPhase.FAILED;
    }

    private boolean terminal(DialogueSessionStatus status) {
        return status == DialogueSessionStatus.COMPLETED
                || status == DialogueSessionStatus.CANCELLED
                || status == DialogueSessionStatus.FAILED;
    }

    private boolean canTrack(DialogueSessionId sessionId) {
        List<DialogueTerminalObserver> tracked = observers.get(sessionId);
        return tracked != null ? tracked.size() < MAX_OBSERVERS_PER_SESSION
                : observers.size() < MAX_OBSERVED_SESSIONS;
    }

    private void notifyTerminal(DialogueSessionView view) {
        List<DialogueTerminalObserver> tracked = observers.remove(view.sessionId());
        if (tracked == null) {
            return;
        }
        DialogueTerminalEvent event = new DialogueTerminalEvent(view);
        tracked.forEach(observer -> notifyOne(observer, event));
    }

    private void notifyOne(DialogueTerminalObserver observer, DialogueTerminalEvent event) {
        try {
            observer.onTerminal(event);
        } catch (RuntimeException exception) {
            diagnosticSink.accept(DialogueDiagnostic.error("EDLG-FORGE-020",
                    DiagnosticCategory.HOST_LIFECYCLE, "Public terminal observer failed."));
        }
    }

    private void removeObserver(DialogueSessionId sessionId, DialogueTerminalObserver observer) {
        List<DialogueTerminalObserver> tracked = observers.get(sessionId);
        if (tracked == null) {
            return;
        }
        tracked.remove(observer);
        if (tracked.isEmpty()) {
            observers.remove(sessionId);
        }
    }
}
