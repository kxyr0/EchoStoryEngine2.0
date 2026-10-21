package io.github.kxyr0.echoquests.api.runtime;

import io.github.kxyr0.echoquests.api.identity.QuestEventId;
import io.github.kxyr0.echoquests.api.identity.QuestId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestService {
    QuestStartResult start(UUID playerId, QuestId questId);
    QuestDispatchResult dispatch(UUID playerId, QuestId questId, QuestEventId eventId);
    Optional<QuestProgress> progress(UUID playerId, QuestId questId);
    List<QuestProgress> progress(UUID playerId);
}
