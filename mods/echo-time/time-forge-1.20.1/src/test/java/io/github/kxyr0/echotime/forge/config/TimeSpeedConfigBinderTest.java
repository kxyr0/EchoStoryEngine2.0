package io.github.kxyr0.echotime.forge.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeSpeedConfigBinderTest {
    private final TimeSpeedConfigBinder binder = new TimeSpeedConfigBinder();

    @Test
    void authoritativeInitOnlyExamplePassesRealParserAndBinder() {
        var result = binder.bind(valid("1.2", "1.0"), "time-speed.es", 12L);
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals("08:30", result.config().orElseThrow().initialTime().formatHHmm());
        assertEquals(12L, result.config().orElseThrow().revision());
        assertEquals(3_600_000L, result.config().orElseThrow().rate().gameMicroseconds());
        assertEquals(1L, result.config().orElseThrow().rate().simulationTicks());
    }

    @Test
    void invalidReplacementCanBeRejectedWithoutDestroyingLastKnownGood() {
        TimeConfigPublication publication = new TimeConfigPublication(
                binder.bind(valid("1.0", "1.0"), "time-speed.es", 12L).config().orElseThrow());
        assertFalse(publication.commit(binder.bind("broken {", "time-speed.es", 13L)));
        assertEquals(12L, publication.active().revision());
        assertFalse(publication.diagnostics().isEmpty());
    }

    @Test
    void typoThatWouldAdvanceMoreThanOneDayPerTickIsRejected() {
        var result = binder.bind(valid("999999999", "1.0"), "time-speed.es", 2L);
        assertFalse(result.successful());
        assertTrue(result.diagnostics().get(0).message().contains("ECHO_TIME_RATE_TOO_HIGH"));
    }

    private String valid(String minutes, String seconds) {
        return """
                namespace JDH.Config.Time;
                class MainTimeSpeed : TimeSpeedConfigResource
                {
                    private void Init()
                    {
                        Id("jdh:main_time");
                        InitialDay(1);
                        InitialTime("08:30");
                        Speed(gameMinutes: %s, realSeconds: %s);
                        Running(true);
                        SyncSun(true);
                        SyncMoon(true);
                        SunDimension("minecraft:overworld");
                    }
                }
                """.formatted(minutes, seconds);
    }
}
