package io.github.kxyr0.echotriggertimeadapter.forge.schedule;

import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import io.github.kxyr0.echotriggertimeadapter.forge.TimeAdapterServices;
import net.minecraft.server.MinecraftServer;

public final class TimeSchedulePublisher {
    private static final String PROVIDER_ID = "echo_trigger_time_adapter:schedule";
    private TimeSubscriptionIndex index = TimeSubscriptionIndex.empty();

    public TimeSubscriptionIndex index() { return index; }

    public boolean refresh(MinecraftServer server, long occurrenceGeneration) {
        var trigger = TimeAdapterServices.trigger(server);
        var time = TimeAdapterServices.time(server);
        if (trigger.isEmpty() || time.isEmpty()) return false;
        long revision = trigger.orElseThrow().subscriptions().catalogRevision();
        if (revision == index.catalogRevision()
                && occurrenceGeneration == index.occurrenceGeneration()) return false;
        TimeSubscriptionIndex candidate = TimeSubscriptionIndex.from(
                trigger.orElseThrow(), occurrenceGeneration);
        boolean accepted = time.orElseThrow().schedule().replace(
                new TimeScheduleUpdate(PROVIDER_ID, revision, candidate.schedule()));
        if (accepted) index = candidate;
        return accepted;
    }
}
