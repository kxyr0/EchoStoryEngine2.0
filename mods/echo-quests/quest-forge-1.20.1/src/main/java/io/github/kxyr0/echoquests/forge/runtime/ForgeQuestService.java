package io.github.kxyr0.echoquests.forge.runtime;

import io.github.kxyr0.echoquests.api.identity.QuestEventId;
import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.runtime.QuestDispatchResult;
import io.github.kxyr0.echoquests.api.runtime.QuestProgress;
import io.github.kxyr0.echoquests.api.runtime.QuestService;
import io.github.kxyr0.echoquests.api.runtime.QuestStartResult;
import io.github.kxyr0.echoquests.core.runtime.QuestStateMachine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class ForgeQuestService implements QuestService {
    private final QuestStateMachine machine;
    private final Runnable dirty;

    ForgeQuestService(QuestStateMachine machine, Runnable dirty) {
        this.machine = machine;
        this.dirty = dirty;
    }

    @Override
    public QuestStartResult start(UUID playerId, QuestId questId) {
        QuestStartResult result = machine.start(playerId, questId);
        if (result.status() == QuestStartResult.Status.STARTED) dirty.run();
        return result;
    }

    @Override
    public QuestDispatchResult dispatch(UUID playerId, QuestId questId, QuestEventId eventId) {
        QuestDispatchResult result = machine.dispatch(playerId, questId, eventId);
        if (result.status() == QuestDispatchResult.Status.TRANSITIONED
                || result.status() == QuestDispatchResult.Status.COMPLETED
                || result.status() == QuestDispatchResult.Status.FAILED) dirty.run();
        return result;
    }

    @Override public Optional<QuestProgress> progress(UUID playerId, QuestId questId) {
        return machine.progress(playerId, questId);
    }
    @Override public List<QuestProgress> progress(UUID playerId) { return machine.progress(playerId); }
}
