package io.github.kxyr0.echotime.forge.api;

import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.wait.TimeWaitResult;

public interface EchoTimeWaitService {
    TimeWaitResult waitUntil(TimeWaitId waitId, TimeOfDay time);

    void complete(TimeWaitId waitId);
}
