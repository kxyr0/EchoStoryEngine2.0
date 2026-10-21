package io.github.kxyr0.echotriggertimeadapter.forge.schedule;

import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import java.util.Optional;

public final class TimeSelectorParser {
    public Optional<TimeSelectorValue> parse(TriggerEventSelector selector) {
        if (!selector.providerId().equals("echo_time:occurrences") || !selector.eventKind().equals("AT")) {
            return Optional.empty();
        }
        String[] parts = selector.scenarioId().split("/");
        try {
            if (parts.length == 2 && parts[0].equals("echo_time:daily")) {
                return Optional.of(new TimeSelectorValue(time(parts[1]), Optional.empty()));
            }
            if (parts.length == 3 && parts[0].equals("echo_time:day")) {
                return Optional.of(new TimeSelectorValue(time(parts[2]),
                        Optional.of(new EchoDay(Long.parseLong(parts[1])))));
            }
        } catch (IllegalArgumentException ignored) { }
        return Optional.empty();
    }

    private TimeOfDay time(String encoded) { return TimeOfDay.parse(encoded.replace('_', ':')); }
}
