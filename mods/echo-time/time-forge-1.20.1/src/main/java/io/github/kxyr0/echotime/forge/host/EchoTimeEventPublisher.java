package io.github.kxyr0.echotime.forge.host;

import io.github.kxyr0.echotime.api.schedule.TimeOccurrenceCrossed;
import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotime.core.state.TimeWait;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeLifecycleEvent;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeLifecyclePhase;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeOccurrenceEvent;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeOccurrenceBatchEvent;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeWaitCompletedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

final class EchoTimeEventPublisher {
    private final MinecraftServer server;
    private final EchoTimeTrace trace;

    EchoTimeEventPublisher(MinecraftServer server, EchoTimeTrace trace) {
        this.server = server;
        this.trace = trace;
    }

    void occurrences(java.util.List<TimeOccurrenceCrossed> values) {
        if (values.isEmpty()) return;
        MinecraftForge.EVENT_BUS.post(new EchoTimeOccurrenceBatchEvent(server, values));
        values.forEach(value -> {
            trace.add("TRIGGER_CROSSED", value.occurrenceId().value());
            MinecraftForge.EVENT_BUS.post(new EchoTimeOccurrenceEvent(server, value));
        });
    }

    void waitCompleted(TimeWait value) {
        trace.add("WAIT_COMPLETED", value.waitId().value());
        MinecraftForge.EVENT_BUS.post(new EchoTimeWaitCompletedEvent(server, value.waitId()));
    }

    void lifecycle(EchoTimeLifecyclePhase phase, EchoTimeSnapshot snapshot) {
        MinecraftForge.EVENT_BUS.post(new EchoTimeLifecycleEvent(server, phase, snapshot));
    }
}
