package io.github.kxyr0.echoplayer.core.mutation;

import io.github.kxyr0.echoplayer.api.mutation.PlayerMutationId;
import java.util.Objects;

/**
 * Durable result for one mutation idempotency key.
 */
public record PlayerMutationReceipt(
        PlayerMutationId id,
        String requestFingerprint,
        String result,
        boolean replayed,
        boolean conflict
) {
    public PlayerMutationReceipt {
        id = Objects.requireNonNull(id, "id");
        requestFingerprint = Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        result = Objects.requireNonNull(result, "result");
    }
}
