package io.github.kxyr0.echoquests.forge.time;

import io.github.kxyr0.echoquests.api.condition.QuestConditionResolver;
import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.identity.QuestStateId;
import io.github.kxyr0.echoquests.api.runtime.QuestDispatchResult;
import io.github.kxyr0.echoquests.api.runtime.QuestStatus;
import io.github.kxyr0.echoquests.core.catalog.QuestCatalog;
import io.github.kxyr0.echoquests.core.runtime.QuestProgressKey;
import io.github.kxyr0.echoquests.core.runtime.QuestStateMachine;
import io.github.kxyr0.echoquests.forge.condition.VanillaQuestConditionResolver;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeOccurrenceBatchEvent;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class QuestTimeCoordinator {
    private static final String PROVIDER_ID = "echo_quests:deadlines";
    private final QuestStateMachine machine;
    private final Runnable dirty;
    private Map<String, Deadline> deadlines = Map.of();
    private String generation = PROVIDER_ID + "/0";

    public QuestTimeCoordinator(QuestStateMachine machine, Runnable dirty) {
        this.machine = machine;
        this.dirty = dirty;
    }

    public boolean publish(MinecraftServer server, QuestCatalog catalog, long revision) {
        Map<String, Deadline> replacement = index(catalog);
        String nextGeneration = PROVIDER_ID + "/" + revision;
        var entries = replacement.entrySet().stream().map(value -> new TimeScheduleEntry(value.getKey(),
                TimeOfDay.parse(value.getValue().time()), Optional.empty(), nextGeneration, 10)).toList();
        boolean accepted = EchoTimeServiceAccess.find(server).map(value -> value.schedule()
                .replace(new TimeScheduleUpdate(PROVIDER_ID, revision, entries))).orElse(false);
        if (accepted) {
            deadlines = Map.copyOf(replacement);
            generation = nextGeneration;
        }
        return accepted;
    }

    public void occurrence(EchoTimeOccurrenceBatchEvent event) {
        long instant = Long.MIN_VALUE;
        Set<QuestProgressKey> checked = new HashSet<>();
        for (var occurrence : event.occurrences()) {
            if (!occurrence.entry().generation().equals(generation)) continue;
            long occurrenceInstant = occurrence.occurrence().absoluteMicroseconds();
            if (occurrenceInstant != instant) {
                checked.clear();
                instant = occurrenceInstant;
            }
            apply(event.server(), occurrence.entry().selectorId(), checked);
        }
    }

    public void clear() {
        deadlines = Map.of();
        generation = PROVIDER_ID + "/0";
    }

    private void apply(MinecraftServer server, String selectorId, Set<QuestProgressKey> checked) {
        Deadline deadline = deadlines.get(selectorId);
        if (deadline == null) return;
        machine.allProgress().stream().filter(value -> value.questId().equals(deadline.questId()))
                .filter(value -> value.stateId().equals(deadline.stateId()))
                .filter(value -> value.status() == QuestStatus.ACTIVE).forEach(value -> {
                    if (!checked.add(new QuestProgressKey(value.playerId(), value.questId()))) return;
                    var player = Optional.ofNullable(server.getPlayerList().getPlayer(value.playerId()));
                    Optional<QuestConditionResolver> resolver = player.map(
                            ignored -> new VanillaQuestConditionResolver(player));
                    QuestDispatchResult result = machine.check(value.playerId(), deadline.questId(),
                            deadline.time(), resolver);
                    if (changed(result.status())) dirty.run();
                });
    }

    private Map<String, Deadline> index(QuestCatalog catalog) {
        Map<String, Deadline> result = new LinkedHashMap<>();
        catalog.definitions().forEach(quest -> quest.states().values().forEach(state ->
                state.timedTransitions().forEach(rule -> {
                    String selector = PROVIDER_ID + "/" + quest.id().value() + "/" + state.id().value()
                            + "/" + rule.time().replace(':', '_');
                    result.put(selector, new Deadline(quest.id(), state.id(), rule.time()));
                })));
        return result;
    }

    private boolean changed(QuestDispatchResult.Status status) {
        return status == QuestDispatchResult.Status.TRANSITIONED
                || status == QuestDispatchResult.Status.COMPLETED
                || status == QuestDispatchResult.Status.FAILED;
    }

    private record Deadline(QuestId questId, QuestStateId stateId, String time) { }
}
