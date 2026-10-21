package io.github.kxyr0.echotime.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.mutation.TimeMutationStatus;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoDuration;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import io.github.kxyr0.echotime.core.state.EchoTimeState;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class EchoTimeRuntimeRestartTest {
    @Test void restoredReceiptPreventsDoubleAddAndOfflineProgression() {
        EchoTimeRuntime first = runtime("09:40");
        TimeAddRequest request = add("story:44", 10);
        first.add(request, 100);
        EchoTimeState saved = first.state();

        EchoTimeRuntime restored = new EchoTimeRuntime(saved, 1024);
        assertEquals("09:50", restored.instant().timeOfDay().formatHHmm());
        assertEquals(TimeMutationStatus.DUPLICATE, restored.add(request, 101).result().status());
        assertEquals("09:50", restored.instant().timeOfDay().formatHHmm());
    }

    @Test void waitsAndPauseLeasesSurviveStateRoundTrip() {
        EchoTimeRuntime first = runtime("09:40");
        first.pause(new TimePauseOwner("story:one"), new TimeMutationId("pause:one"));
        first.waitUntil(new TimeWaitId("wait:one"), TimeOfDay.parse("09:50"));

        EchoTimeRuntime restored = new EchoTimeRuntime(first.state(), 1024);
        assertTrue(restored.snapshot(100).paused());
        restored.resume(new TimePauseOwner("story:one"));
        restored.add(add("story:advance", 10), 101);
        assertEquals(List.of("wait:one"), restored.completedWaits().stream()
                .map(value -> value.waitId().value()).toList());
    }

    private EchoTimeRuntime runtime(String time) {
        TimeRate rate = TimeRate.compile(BigDecimal.ONE, BigDecimal.ONE);
        EchoInstant instant = EchoInstant.at(EchoDay.FIRST, TimeOfDay.parse(time), TimeRevision.INITIAL);
        return new EchoTimeRuntime(new EchoTimeState(1, instant, rate, 0, true, 1,
                List.of(), List.of(), List.of()), 1024);
    }

    private TimeAddRequest add(String id, long minutes) {
        return new TimeAddRequest(new TimeMutationId(id), EchoDuration.minutes(minutes),
                TimeCrossingPolicy.FIRE_CROSSED, TimeRequestSource.command(id, "restart test"));
    }
}
