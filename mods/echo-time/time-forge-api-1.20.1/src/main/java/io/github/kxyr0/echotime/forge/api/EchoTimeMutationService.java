package io.github.kxyr0.echotime.forge.api;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.pause.TimePauseResult;

public interface EchoTimeMutationService {
    TimeMutationResult set(TimeSetRequest request);

    TimeMutationResult add(TimeAddRequest request);

    TimePauseResult pause(TimePauseOwner owner, TimeMutationId mutationId);

    TimePauseResult resume(TimePauseOwner owner, TimeMutationId mutationId);
}
