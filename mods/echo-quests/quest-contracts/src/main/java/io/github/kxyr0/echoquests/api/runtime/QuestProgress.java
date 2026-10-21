package io.github.kxyr0.echoquests.api.runtime;

import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.identity.QuestStateId;
import java.util.Objects;
import java.util.UUID;

public record QuestProgress(
        UUID playerId,
        QuestId questId,
        QuestStateId stateId,
        QuestStatus status,
        long revision) {

    public QuestProgress {
        playerId = Objects.requireNonNull(playerId, "playerId");
        questId = Objects.requireNonNull(questId, "questId");
        stateId = Objects.requireNonNull(stateId, "stateId");
        status = Objects.requireNonNull(status, "status");
        if (revision < 1) throw new IllegalArgumentException("Quest revision must be positive.");
    }
}
