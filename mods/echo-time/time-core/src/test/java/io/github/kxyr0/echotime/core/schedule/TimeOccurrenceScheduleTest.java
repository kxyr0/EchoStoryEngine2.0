package io.github.kxyr0.echotime.core.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotime.api.schedule.TimeCrossingReason;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TimeOccurrenceScheduleTest {
    @Test
    void skippedEqualityStillCrossesExactlyOnce() {
        TimeOccurrenceSchedule schedule = schedule(daily("warning", "09:50"));

        var result = schedule.crossed(instant(1, "09:49"), instant(1, "09:51"),
                TimeCrossingReason.NATURAL_ADVANCE, 1024);

        assertEquals(1, result.occurrences().size());
        assertEquals("09:50", result.occurrences().get(0).occurrence().timeOfDay().formatHHmm());
    }

    @Test
    void multipleBucketsAndMidnightAreChronological() {
        TimeOccurrenceSchedule schedule = schedule(
                specific("late", 1, "23:59"), daily("midnight", "00:00"),
                specific("early", 2, "00:02"));

        var result = schedule.crossed(instant(1, "23:58"), instant(2, "00:03"),
                TimeCrossingReason.NATURAL_ADVANCE, 1024);

        assertEquals(List.of("late", "midnight", "early"), result.occurrences().stream()
                .map(value -> value.entry().selectorId()).toList());
    }

    @Test
    void boundedCrossingNeverReturnsPartialSubset() {
        TimeOccurrenceSchedule schedule = schedule(
                daily("a", "09:49"), daily("b", "09:50"), daily("c", "09:52"));

        var result = schedule.crossed(instant(1, "09:48"), instant(1, "09:53"),
                TimeCrossingReason.SCRIPT_TIME_JUMP, 2);

        assertTrue(result.limitExceeded());
        assertTrue(result.occurrences().isEmpty());
    }

    @Test
    void registrationInThePastDoesNotImplicitlyCatchUp() {
        TimeOccurrenceSchedule schedule = schedule(daily("warning", "09:50"));
        assertFalse(schedule.crossed(instant(1, "10:30"), instant(1, "10:31"),
                TimeCrossingReason.NATURAL_ADVANCE, 1024).limitExceeded());
        assertTrue(schedule.crossed(instant(1, "10:30"), instant(1, "10:31"),
                TimeCrossingReason.NATURAL_ADVANCE, 1024).occurrences().isEmpty());
    }

    @Test
    void multiDayJumpProducesDistinctDailyOccurrencesWithoutMinuteIteration() {
        TimeOccurrenceSchedule schedule = schedule(daily("warning", "09:50"));
        var result = schedule.crossed(instant(1, "10:00"), instant(4, "10:00"),
                TimeCrossingReason.SCRIPT_TIME_JUMP, 1024);
        assertEquals(List.of(2L, 3L, 4L), result.occurrences().stream()
                .map(value -> value.occurrence().day().value()).toList());
        assertEquals(3, result.occurrences().stream().map(value -> value.occurrenceId().value()).distinct().count());
    }

    @Test
    void enormousJumpIsRejectedBeforeOccurrenceExpansion() {
        TimeOccurrenceSchedule schedule = schedule(daily("warning", "09:50"));
        var result = schedule.crossed(instant(1, "09:00"), instant(1_000_000, "10:00"),
                TimeCrossingReason.SCRIPT_TIME_JUMP, 1024);
        assertTrue(result.limitExceeded());
        assertTrue(result.occurrences().isEmpty());
    }

    private TimeOccurrenceSchedule schedule(TimeScheduleEntry... entries) {
        TimeOccurrenceSchedule value = new TimeOccurrenceSchedule();
        value.replace(new TimeScheduleUpdate("test:provider", 1L, List.of(entries)));
        return value;
    }

    private TimeScheduleEntry daily(String id, String time) {
        return new TimeScheduleEntry(id, TimeOfDay.parse(time), Optional.empty(), "v1", 0);
    }

    private TimeScheduleEntry specific(String id, long day, String time) {
        return new TimeScheduleEntry(id, TimeOfDay.parse(time), Optional.of(new EchoDay(day)), "v1", 0);
    }

    private EchoInstant instant(long day, String time) {
        return EchoInstant.at(new EchoDay(day), TimeOfDay.parse(time), TimeRevision.INITIAL);
    }
}
