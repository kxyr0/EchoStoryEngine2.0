package io.github.kxyr0.echoquests.core.runtime;

import io.github.kxyr0.echoquests.api.identity.QuestEventId;
import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.condition.QuestConditionResolver;
import io.github.kxyr0.echoquests.api.model.QuestStateDefinition;
import io.github.kxyr0.echoquests.api.model.QuestStateKind;
import io.github.kxyr0.echoquests.api.runtime.QuestDispatchResult;
import io.github.kxyr0.echoquests.api.runtime.QuestProgress;
import io.github.kxyr0.echoquests.api.runtime.QuestService;
import io.github.kxyr0.echoquests.api.runtime.QuestStartResult;
import io.github.kxyr0.echoquests.api.runtime.QuestStatus;
import io.github.kxyr0.echoquests.core.catalog.QuestCatalog;
import io.github.kxyr0.echoquests.core.condition.QuestConditions;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class QuestStateMachine implements QuestService {
    private QuestCatalog catalog = QuestCatalog.empty();
    private final Map<QuestProgressKey, QuestProgress> progress = new LinkedHashMap<>();

    public synchronized void replaceCatalog(QuestCatalog replacement) { catalog = replacement; }

    @Override
    public synchronized QuestStartResult start(UUID playerId, QuestId questId) {
        var definition = catalog.find(questId);
        if (definition.isEmpty()) return start(QuestStartResult.Status.UNKNOWN_QUEST, Optional.empty(), questId);
        QuestProgress existing = progress.get(new QuestProgressKey(playerId, questId));
        if (existing != null) {
            var status = existing.status() == QuestStatus.ACTIVE ? QuestStartResult.Status.ALREADY_ACTIVE
                    : QuestStartResult.Status.ALREADY_TERMINAL;
            return start(status, Optional.of(existing), questId);
        }
        QuestProgress created = new QuestProgress(playerId, questId, definition.orElseThrow().initialState(),
                QuestStatus.ACTIVE, 1);
        progress.put(new QuestProgressKey(playerId, questId), created);
        return start(QuestStartResult.Status.STARTED, Optional.of(created), questId);
    }

    @Override
    public synchronized QuestDispatchResult dispatch(UUID playerId, QuestId questId, QuestEventId eventId) {
        var definition = catalog.find(questId);
        if (definition.isEmpty()) return dispatch(QuestDispatchResult.Status.UNKNOWN_QUEST, Optional.empty(), eventId);
        QuestProgress current = progress.get(new QuestProgressKey(playerId, questId));
        if (current == null) return dispatch(QuestDispatchResult.Status.NOT_STARTED, Optional.empty(), eventId);
        if (current.status() != QuestStatus.ACTIVE) {
            return dispatch(QuestDispatchResult.Status.NOT_ACTIVE, Optional.of(current), eventId);
        }
        QuestStateDefinition state = definition.orElseThrow().states().get(current.stateId());
        if (state == null) return dispatch(QuestDispatchResult.Status.NOT_ACTIVE, Optional.of(current), eventId);
        var targetId = state.transitions().get(eventId);
        if (targetId == null) return dispatch(QuestDispatchResult.Status.NO_TRANSITION, Optional.of(current), eventId);
        return move(current, definition.orElseThrow().states().get(targetId), eventId);
    }

    public synchronized QuestDispatchResult check(UUID playerId, QuestId questId, String time,
            QuestConditionResolver resolver) {
        return check(playerId, questId, time, Optional.of(resolver));
    }

    public synchronized QuestDispatchResult check(UUID playerId, QuestId questId, String time,
            Optional<QuestConditionResolver> resolver) {
        var definition = catalog.find(questId);
        QuestEventId event = QuestEventId.parse("time/" + time.replace(':', '_'));
        if (definition.isEmpty()) return dispatch(QuestDispatchResult.Status.UNKNOWN_QUEST, Optional.empty(), event);
        QuestProgress current = progress.get(new QuestProgressKey(playerId, questId));
        if (current == null) return dispatch(QuestDispatchResult.Status.NOT_STARTED, Optional.empty(), event);
        if (current.status() != QuestStatus.ACTIVE) {
            return dispatch(QuestDispatchResult.Status.NOT_ACTIVE, Optional.of(current), event);
        }
        QuestStateDefinition state = definition.orElseThrow().states().get(current.stateId());
        if (state == null) return dispatch(QuestDispatchResult.Status.NOT_ACTIVE, Optional.of(current), event);
        var timed = state.timedTransitions().stream().filter(value -> value.time().equals(time)).findFirst();
        if (timed.isEmpty()) return dispatch(QuestDispatchResult.Status.NO_TRANSITION, Optional.of(current), event);
        var rule = timed.orElseThrow();
        var targetId = resolver.map(value -> QuestConditions.test(rule.condition(), value)).orElse(false)
                ? rule.satisfiedState() : rule.otherwiseState();
        return move(current, definition.orElseThrow().states().get(targetId), event);
    }

    @Override
    public synchronized Optional<QuestProgress> progress(UUID playerId, QuestId questId) {
        return Optional.ofNullable(progress.get(new QuestProgressKey(playerId, questId)));
    }

    @Override
    public synchronized List<QuestProgress> progress(UUID playerId) {
        return progress.values().stream().filter(value -> value.playerId().equals(playerId))
                .sorted((left, right) -> left.questId().compareTo(right.questId())).toList();
    }

    public synchronized Map<QuestProgressKey, QuestProgress> entries() { return Map.copyOf(progress); }

    public synchronized List<QuestProgress> allProgress() {
        return progress.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
    }

    public synchronized void restore(Collection<QuestProgress> values) {
        progress.clear();
        values.forEach(value -> progress.put(new QuestProgressKey(value.playerId(), value.questId()), value));
    }

    private QuestStatus status(QuestStateKind kind) {
        return switch (kind) {
            case ACTIVE -> QuestStatus.ACTIVE;
            case COMPLETED -> QuestStatus.COMPLETED;
            case FAILED -> QuestStatus.FAILED;
        };
    }

    private QuestDispatchResult.Status result(QuestStateKind kind) {
        return switch (kind) {
            case ACTIVE -> QuestDispatchResult.Status.TRANSITIONED;
            case COMPLETED -> QuestDispatchResult.Status.COMPLETED;
            case FAILED -> QuestDispatchResult.Status.FAILED;
        };
    }

    private QuestDispatchResult move(QuestProgress current, QuestStateDefinition target, QuestEventId event) {
        QuestProgress changed = new QuestProgress(current.playerId(), current.questId(), target.id(),
                status(target.kind()), current.revision() + 1);
        progress.put(new QuestProgressKey(current.playerId(), current.questId()), changed);
        return dispatch(result(target.kind()), Optional.of(changed), event);
    }

    private QuestStartResult start(QuestStartResult.Status status, Optional<QuestProgress> value, QuestId id) {
        return new QuestStartResult(status, value, status + ": " + id);
    }

    private QuestDispatchResult dispatch(QuestDispatchResult.Status status, Optional<QuestProgress> value,
            QuestEventId event) {
        return new QuestDispatchResult(status, value, status + ": " + event);
    }
}
