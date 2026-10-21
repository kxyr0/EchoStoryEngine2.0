package io.github.kxyr0.echoquests.core.runtime;

import io.github.kxyr0.echoquests.api.condition.QuestCondition;
import io.github.kxyr0.echoquests.api.identity.QuestEventId;
import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.identity.QuestStateId;
import io.github.kxyr0.echoquests.api.model.QuestDefinition;
import io.github.kxyr0.echoquests.api.model.QuestStateDefinition;
import io.github.kxyr0.echoquests.api.model.QuestStateKind;
import io.github.kxyr0.echoquests.api.model.QuestTimedTransition;
import io.github.kxyr0.echoquests.api.runtime.QuestDispatchResult;
import io.github.kxyr0.echoquests.api.runtime.QuestStartResult;
import io.github.kxyr0.echoquests.api.runtime.QuestStatus;
import io.github.kxyr0.echoquests.core.catalog.QuestCatalog;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class QuestStateMachineTest {
    private static final QuestId QUEST = QuestId.parse("jdh:find_notebook");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final UUID OTHER_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000043");

    @Test
    void transitionsArePlayerScopedIdempotentAndTerminal() {
        QuestStateMachine machine = new QuestStateMachine();
        machine.replaceCatalog(QuestCatalog.of(List.of(definition())));

        assertEquals(QuestStartResult.Status.STARTED, machine.start(PLAYER, QUEST).status());
        assertEquals(QuestStartResult.Status.ALREADY_ACTIVE, machine.start(PLAYER, QUEST).status());
        assertEquals(QuestDispatchResult.Status.TRANSITIONED,
                machine.dispatch(PLAYER, QUEST, QuestEventId.parse("notebook_found")).status());
        assertEquals(QuestDispatchResult.Status.COMPLETED,
                machine.dispatch(PLAYER, QUEST, QuestEventId.parse("notebook_returned")).status());
        assertEquals(QuestStatus.COMPLETED, machine.progress(PLAYER, QUEST).orElseThrow().status());
        assertEquals(3, machine.progress(PLAYER, QUEST).orElseThrow().revision());
        assertEquals(QuestDispatchResult.Status.NOT_ACTIVE,
                machine.dispatch(PLAYER, QUEST, QuestEventId.parse("notebook_returned")).status());
        assertEquals(QuestStartResult.Status.ALREADY_TERMINAL, machine.start(PLAYER, QUEST).status());
    }

    @Test
    void timedCheckUsesSatisfiedOrFallbackState() {
        QuestStateId waiting = new QuestStateId("waiting");
        QuestStateId completed = new QuestStateId("completed");
        QuestStateId failed = new QuestStateId("failed");
        var condition = QuestCondition.leaf("test:has_item", Map.of());
        var rule = new QuestTimedTransition("18:00", condition, completed, failed);
        QuestDefinition definition = new QuestDefinition(QUEST, "Deadline", waiting, Map.of(
                waiting, new QuestStateDefinition(waiting, "Arrive prepared", QuestStateKind.ACTIVE,
                        Map.of(), List.of(rule)),
                completed, new QuestStateDefinition(completed, "", QuestStateKind.COMPLETED,
                        Map.of(), List.of()),
                failed, new QuestStateDefinition(failed, "", QuestStateKind.FAILED, Map.of(), List.of())));
        QuestStateMachine machine = new QuestStateMachine();
        machine.replaceCatalog(QuestCatalog.of(List.of(definition)));
        machine.start(PLAYER, QUEST);

        assertEquals(QuestDispatchResult.Status.FAILED,
                machine.check(PLAYER, QUEST, "18:00", ignored -> false).status());
        assertEquals(QuestStatus.FAILED, machine.progress(PLAYER, QUEST).orElseThrow().status());
        machine.start(OTHER_PLAYER, QUEST);
        assertEquals(QuestDispatchResult.Status.COMPLETED,
                machine.check(OTHER_PLAYER, QUEST, "18:00", ignored -> true).status());
    }

    @Test
    void unavailablePlayerContextAlwaysUsesFallbackEvenForNot() {
        QuestStateId waiting = new QuestStateId("waiting");
        QuestStateId completed = new QuestStateId("completed");
        QuestStateId failed = new QuestStateId("failed");
        var missing = QuestCondition.leaf("test:has_item", Map.of());
        var condition = new QuestCondition("not", Map.of(), List.of(missing));
        var rule = new QuestTimedTransition("18:00", condition, completed, failed);
        QuestDefinition definition = new QuestDefinition(QUEST, "Offline deadline", waiting, Map.of(
                waiting, new QuestStateDefinition(waiting, "Be online", QuestStateKind.ACTIVE,
                        Map.of(), List.of(rule)),
                completed, new QuestStateDefinition(completed, "", QuestStateKind.COMPLETED, Map.of(), List.of()),
                failed, new QuestStateDefinition(failed, "", QuestStateKind.FAILED, Map.of(), List.of())));
        QuestStateMachine machine = new QuestStateMachine();
        machine.replaceCatalog(QuestCatalog.of(List.of(definition)));
        machine.start(PLAYER, QUEST);

        assertEquals(QuestDispatchResult.Status.FAILED,
                machine.check(PLAYER, QUEST, "18:00", Optional.empty()).status());
    }

    private QuestDefinition definition() {
        var find = new QuestStateId("find");
        var back = new QuestStateId("return");
        var done = new QuestStateId("done");
        return new QuestDefinition(QUEST, "Потерянный блокнот", find, Map.of(
                find, new QuestStateDefinition(find, "Найдите блокнот", QuestStateKind.ACTIVE,
                        Map.of(new QuestEventId("notebook_found"), back), List.of()),
                back, new QuestStateDefinition(back, "Верните блокнот", QuestStateKind.ACTIVE,
                        Map.of(new QuestEventId("notebook_returned"), done), List.of()),
                done, new QuestStateDefinition(done, "", QuestStateKind.COMPLETED, Map.of(), List.of())));
    }
}
