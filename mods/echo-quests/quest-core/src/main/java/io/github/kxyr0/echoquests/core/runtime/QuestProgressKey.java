package io.github.kxyr0.echoquests.core.runtime;

import io.github.kxyr0.echoquests.api.identity.QuestId;
import java.util.Objects;
import java.util.UUID;

public record QuestProgressKey(UUID playerId, QuestId questId) implements Comparable<QuestProgressKey> {
    public QuestProgressKey {
        playerId = Objects.requireNonNull(playerId, "playerId");
        questId = Objects.requireNonNull(questId, "questId");
    }

    @Override
    public int compareTo(QuestProgressKey other) {
        int playerOrder = playerId.toString().compareTo(other.playerId.toString());
        return playerOrder != 0 ? playerOrder : questId.compareTo(other.questId);
    }
}
