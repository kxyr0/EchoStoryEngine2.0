package io.github.kxyr0.echotime.forge.host;

import io.github.kxyr0.echotime.api.config.TimeSpeedConfig;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import io.github.kxyr0.echotime.core.runtime.EchoTimeRuntime;
import io.github.kxyr0.echotime.core.state.EchoTimeState;
import java.math.BigDecimal;
import java.util.List;

final class EchoTimeBootstrap {
    private EchoTimeBootstrap() { }

    static EchoTimeState initialState(TimeSpeedConfig config) {
        EchoInstant instant = EchoInstant.at(config.initialDay(), config.initialTime(), TimeRevision.INITIAL);
        return new EchoTimeState(EchoTimeRuntime.STATE_SCHEMA, instant, config.rate(), 0L, config.running(),
                config.revision(), List.of(), List.of(), List.of());
    }

    static TimeSpeedConfig configFromState(EchoTimeState state) {
        return new TimeSpeedConfig("jdh:persisted_time", state.instant().day(), state.instant().timeOfDay(),
                state.rate(), state.configuredRunning(), true, true, List.of("minecraft:overworld"),
                state.configRevision(), 1024);
    }

    static TimeSpeedConfig defaultConfig() {
        return new TimeSpeedConfig("jdh:main_time", EchoDay.FIRST, TimeOfDay.parse("08:30"),
                TimeRate.compile(BigDecimal.ONE, BigDecimal.ONE), true, true, true,
                List.of("minecraft:overworld"), 1L, 1024);
    }
}
