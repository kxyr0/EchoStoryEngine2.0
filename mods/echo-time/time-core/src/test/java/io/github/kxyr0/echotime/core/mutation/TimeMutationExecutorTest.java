package io.github.kxyr0.echotime.core.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.mutation.TimeMutationStatus;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoDuration;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import io.github.kxyr0.echotime.core.clock.EchoClock;
import io.github.kxyr0.echotime.core.schedule.TimeOccurrenceSchedule;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeMutationExecutorTest {
    private EchoClock clock;
    private TimeMutationLedger ledger;
    private TimeMutationExecutor executor;

    @BeforeEach
    void setUp() {
        clock = new EchoClock(instant("09:40"), TimeRate.compile(BigDecimal.ONE, BigDecimal.ONE), 0L, true, 1L);
        TimeOccurrenceSchedule schedule = new TimeOccurrenceSchedule();
        schedule.replace(new TimeScheduleUpdate("test:provider", 1L, List.of(new TimeScheduleEntry(
                "warning", TimeOfDay.parse("09:50"), Optional.empty(), "v1", 0))));
        ledger = new TimeMutationLedger();
        executor = new TimeMutationExecutor(clock, schedule, ledger, 1024);
    }

    @Test
    void addFiresCrossedByDefaultRequestAndRollsForward() {
        MutationExecution result = executor.add(add("story:44", 10), 100L);
        assertEquals("09:50", result.result().current().timeOfDay().formatHHmm());
        assertEquals(1, result.occurrences().size());
    }

    @Test
    void recoveredAddWithSameMutationIdDoesNotApplyTwice() {
        executor.add(add("story:44", 10), 100L);
        MutationExecution recovered = executor.add(add("story:44", 10), 101L);
        assertEquals(TimeMutationStatus.DUPLICATE, recovered.result().status());
        assertEquals("09:50", clock.instant().timeOfDay().formatHHmm());
        assertEquals(1, ledger.receipts().size());
    }

    @Test
    void setSkipsCrossedAndCanRewindSameDay() {
        TimeSetRequest request = new TimeSetRequest(new TimeMutationId("story:set"), Optional.empty(),
                TimeOfDay.parse("09:00"), TimeCrossingPolicy.SKIP_CROSSED, source());
        MutationExecution result = executor.set(request, 100L);
        assertTrue(result.occurrences().isEmpty());
        assertEquals("09:00", clock.instant().timeOfDay().formatHHmm());
    }

    private TimeAddRequest add(String id, long minutes) {
        return new TimeAddRequest(new TimeMutationId(id), EchoDuration.minutes(minutes),
                TimeCrossingPolicy.FIRE_CROSSED, source());
    }

    private TimeRequestSource source() {
        return TimeRequestSource.command("story:44", "test");
    }

    private EchoInstant instant(String value) {
        return EchoInstant.at(EchoDay.FIRST, TimeOfDay.parse(value), TimeRevision.INITIAL);
    }
}
