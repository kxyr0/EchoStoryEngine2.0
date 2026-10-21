package io.github.kxyr0.echodialogue.forge.host;

import io.github.kxyr0.echodialogue.api.failure.DialogueFailureRequest;
import io.github.kxyr0.echodialogue.api.failure.DialogueFailureResult;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.observation.DialogueObservationHandle;
import io.github.kxyr0.echodialogue.api.observation.DialogueTerminalObserver;
import io.github.kxyr0.echodialogue.api.service.DialogueCancelResult;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueQueryResult;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.api.service.DialogueStartResult;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWaitRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWaitResult;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.forge.domain.DialogueCommitPolicy;
import io.github.kxyr0.echodialogue.forge.domain.DialogueFailureEndpoint;
import io.github.kxyr0.echodialogue.forge.service.DialoguePublicResults;
import io.github.kxyr0.echodialogue.forge.service.DialoguePublicSessionReadModel;
import io.github.kxyr0.echodialogue.forge.service.DialoguePublicViewResolver;
import java.util.List;
import java.util.Optional;

/**
 * Public server-scoped service facade backed by one active Forge dialogue host.
 */
final class ForgeDialoguePublicService implements DialogueService {
    private final ForgeDialogueHost host;
    private final DialoguePublicSessionReadModel readModel;
    private final DialoguePublicViewResolver views;

    ForgeDialoguePublicService(ForgeDialogueHost host, DialoguePublicSessionReadModel readModel) {
        this.host = host;
        this.readModel = readModel;
        this.views = new DialoguePublicViewResolver(host.sessions, host.repositoryRevisions, readModel);
    }

    @Override
    public DialogueStartResult start(DialogueStartRequest request) {
        if (!available()) {
            return new DialogueStartResult(DialogueOperationStatus.SERVICE_UNAVAILABLE, Optional.empty(), List.of());
        }
        Optional<DialogueSessionView> existing = readModel.pendingOrRetained(request.sessionId())
                .or(() -> views.bySessionId(request.sessionId()));
        if (existing.isPresent()) {
            return DialoguePublicResults.existingStart(request, existing.orElseThrow());
        }
        Optional<DialogueSessionView> busy = views.byAudienceId(request.audienceId())
                .filter(DialoguePublicResults::activeOrWaiting);
        if (busy.isPresent()) {
            return new DialogueStartResult(DialogueOperationStatus.CONFLICT, busy, List.of());
        }
        return host.catalogState.current().find(request.dialogueId())
                .map(dialogue -> startFound(request, dialogue))
                .orElseGet(() -> new DialogueStartResult(DialogueOperationStatus.NOT_FOUND, Optional.empty(),
                        List.of(DialoguePublicResults.missingDiagnostic(request.dialogueId()))));
    }

    @Override
    public DialogueQueryResult query(DialogueSessionId sessionId) {
        if (!available()) {
            return new DialogueQueryResult(DialogueOperationStatus.SERVICE_UNAVAILABLE, Optional.empty(), List.of());
        }
        return DialoguePublicResults.query(readModel.pendingOrRetained(sessionId)
                .or(() -> views.bySessionId(sessionId)));
    }

    @Override
    public DialogueCancelResult cancel(DialogueSessionId sessionId) {
        if (!available()) {
            return new DialogueCancelResult(DialogueOperationStatus.SERVICE_UNAVAILABLE, Optional.empty(), List.of());
        }
        Optional<DialogueSessionView> view = readModel.pendingOrRetained(sessionId)
                .or(() -> views.bySessionId(sessionId));
        if (view.isEmpty()) {
            return new DialogueCancelResult(DialogueOperationStatus.NOT_FOUND, Optional.empty(), List.of());
        }
        DialogueSessionView current = view.orElseThrow();
        if (!DialoguePublicResults.activeOrWaiting(current)) {
            return new DialogueCancelResult(DialoguePublicResults.status(current), view, List.of());
        }
        return cancelActive(sessionId, current);
    }

    @Override
    public DialogueExternalWaitResult waitExternal(DialogueExternalWaitRequest request) {
        if (!available()) { return externalUnavailable(); }
        Optional<DialogueSessionState> state = host.sessions.stateBySessionId(request.sessionId());
        if (state.isEmpty()) { return externalMissing(request.sessionId()); }
        DialogueSessionView current = views.view(state.orElseThrow());
        if (current.status() != DialogueSessionStatus.ACTIVE) {
            return DialoguePublicResults.external(DialoguePublicResults.status(current), Optional.of(current),
                    List.of());
        }
        Optional<DialogueTransition> transition = host.sessions.previewWaitExternal(current.audienceId(),
                request.sessionId(), request.expectedRevision(), request.externalWait());
        return transition.map(this::saveExternal).orElseGet(() -> externalMissing(request.sessionId()));
    }

    @Override
    public DialogueExternalWaitResult resumeExternal(DialogueExternalResumeRequest request) {
        if (!available()) { return externalUnavailable(); }
        Optional<DialogueSessionState> state = host.sessions.stateBySessionId(request.sessionId());
        if (state.isEmpty()) { return externalMissing(request.sessionId()); }
        DialogueSessionView current = views.view(state.orElseThrow());
        if (current.status() != DialogueSessionStatus.WAITING) {
            return DialoguePublicResults.external(DialoguePublicResults.status(current), Optional.of(current),
                    List.of());
        }
        Optional<DialogueTransition> transition = host.sessions.previewResumeExternal(current.audienceId(),
                request.sessionId(), request.expectedRevision(), request.waitKey());
        return transition.map(this::saveExternal).orElseGet(() -> externalMissing(request.sessionId()));
    }

    @Override
    public DialogueObservationHandle observeTerminal(DialogueSessionId sessionId, DialogueTerminalObserver observer) {
        return readModel.observeTerminal(sessionId, observer);
    }

    @Override
    public boolean isDurablyAccepted(DialogueSessionId sessionId) {
        return available() && host.repositoryRevisions.containsKey(sessionId);
    }

    private DialogueStartResult startFound(DialogueStartRequest request, CompiledDialogue dialogue) {
        DialogueTransition transition = host.sessions.previewStart(request.audienceId(), request.sessionId(), dialogue);
        DialogueSessionView view = views.view(transition.state());
        if (!DialogueCommitPolicy.initial(transition.status())) {
            return new DialogueStartResult(DialogueOperationStatus.REJECTED, Optional.of(view),
                    transition.diagnostics());
        }
        readModel.pending(transition.state());
        host.saveFlow.domainEvents.remember(request);
        host.saveFlow.queueInitialSave(host.players.apply(request.audienceId()), request.audienceId(), dialogue,
                transition);
        return new DialogueStartResult(DialogueOperationStatus.ACCEPTED, Optional.of(view), transition.diagnostics());
    }

    private DialogueCancelResult cancelActive(DialogueSessionId sessionId, DialogueSessionView current) {
        Optional<DialogueTransition> transition = host.sessions.preview(current.audienceId(),
                DialogueInput.cancel(sessionId, current.revision()));
        transition.ifPresent(value -> host.saveFlow.saveOrDeliver(host.players.apply(value.state().audienceId()),
                value.state().audienceId(), value));
        return transition.map(value -> new DialogueCancelResult(DialogueOperationStatus.ACCEPTED,
                        Optional.of(views.view(value.state())), value.diagnostics()))
                .orElseGet(() -> new DialogueCancelResult(DialogueOperationStatus.NOT_FOUND, Optional.empty(),
                        List.of()));
    }

    private DialogueExternalWaitResult saveExternal(DialogueTransition transition) {
        DialogueSessionView view = views.view(transition.state());
        if (!DialogueCommitPolicy.acceptsExternal(transition.status())) {
            return DialoguePublicResults.external(DialogueOperationStatus.REJECTED, Optional.of(view),
                    transition.diagnostics());
        }
        readModel.pending(transition.state());
        host.saveFlow.saveOrDeliver(host.players.apply(transition.state().audienceId()),
                transition.state().audienceId(), transition);
        return DialoguePublicResults.external(DialogueOperationStatus.ACCEPTED, Optional.of(view),
                transition.diagnostics());
    }

    @Override
    public DialogueFailureResult fail(DialogueFailureRequest request) {
        if (!available()) { return DialogueFailureResult.unavailable(); }
        return new DialogueFailureEndpoint(host.sessions, this::saveExternal, views::view).fail(request);
    }

    private DialogueExternalWaitResult externalUnavailable() {
        return DialoguePublicResults.external(DialogueOperationStatus.SERVICE_UNAVAILABLE, Optional.empty(), List.of());
    }

    private DialogueExternalWaitResult externalMissing(DialogueSessionId sessionId) {
        return DialoguePublicResults.externalMissing(readModel.pendingOrRetained(sessionId));
    }

    private boolean available() {
        return !host.stopped && host.recovered;
    }
}
