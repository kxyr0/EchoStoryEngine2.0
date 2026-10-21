package io.github.kxyr0.echotriggertimeadapter.forge.schedule;

import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.Optional;

public record TimeSelectorValue(TimeOfDay time, Optional<EchoDay> day) { }
