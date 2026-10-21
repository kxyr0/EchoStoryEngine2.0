package io.github.kxyr0.echobackgroundnpc.core.simulation;

import io.github.kxyr0.echobackgroundnpc.api.conversation.ConversationOutcome;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import java.util.Objects;

public record ScheduledConversation(
        BackgroundNpcId initiator,
        BackgroundTask.Converse task,
        BackgroundScheduleEntry scheduleEntry,
        String occurrenceId,
        ConversationOutcome outcome
) {
    public ScheduledConversation {
        initiator = Objects.requireNonNull(initiator, "initiator");
        task = Objects.requireNonNull(task, "task");
        scheduleEntry = Objects.requireNonNull(scheduleEntry, "scheduleEntry");
        occurrenceId = Objects.requireNonNull(occurrenceId, "occurrenceId");
        outcome = Objects.requireNonNull(outcome, "outcome");
    }
}
