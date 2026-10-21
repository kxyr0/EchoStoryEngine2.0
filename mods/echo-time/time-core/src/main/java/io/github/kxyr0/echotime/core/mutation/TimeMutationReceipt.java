package io.github.kxyr0.echotime.core.mutation;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import java.util.Objects;

public record TimeMutationReceipt(
        TimeMutationId mutationId,
        String signature,
        TimeRequestSource source,
        TimeMutationResult result) {
    public TimeMutationReceipt {
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
        signature = Objects.requireNonNull(signature, "signature");
        source = Objects.requireNonNull(source, "source");
        result = Objects.requireNonNull(result, "result");
    }
}
