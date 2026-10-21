package io.github.kxyr0.echotime.script.action;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import java.util.Objects;

public record TimeActionContext(
        TimeMutationId mutationId,
        TimePauseOwner pauseOwner,
        TimeWaitId waitId,
        TimeRequestSource source) {
    public TimeActionContext {
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
        pauseOwner = Objects.requireNonNull(pauseOwner, "pauseOwner");
        waitId = Objects.requireNonNull(waitId, "waitId");
        source = Objects.requireNonNull(source, "source");
    }
}
