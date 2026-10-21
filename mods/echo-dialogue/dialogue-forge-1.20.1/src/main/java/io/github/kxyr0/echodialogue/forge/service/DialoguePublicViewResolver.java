package io.github.kxyr0.echodialogue.forge.service;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import java.util.Map;
import java.util.Optional;

public final class DialoguePublicViewResolver {
    private final ActiveDialogueSessions sessions;
    private final Map<DialogueSessionId, DialogueRepositoryRevision> repositoryRevisions;
    private final DialoguePublicSessionReadModel readModel;

    public DialoguePublicViewResolver(
            ActiveDialogueSessions sessions,
            Map<DialogueSessionId, DialogueRepositoryRevision> repositoryRevisions,
            DialoguePublicSessionReadModel readModel
    ) {
        this.sessions = sessions;
        this.repositoryRevisions = repositoryRevisions;
        this.readModel = readModel;
    }

    public Optional<DialogueSessionView> bySessionId(DialogueSessionId sessionId) {
        return sessions.stateBySessionId(sessionId).map(this::view);
    }

    public Optional<DialogueSessionView> byAudienceId(AudienceId audienceId) {
        return sessions.stateFor(audienceId).map(this::view);
    }

    public DialogueSessionView view(DialogueSessionState state) {
        return readModel.view(state, revision(state.sessionId()));
    }

    public DialogueRepositoryRevision revision(DialogueSessionId sessionId) {
        return repositoryRevisions.getOrDefault(sessionId, new DialogueRepositoryRevision(0));
    }
}
