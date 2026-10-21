package io.github.kxyr0.echoplayer.core.mutation;

import io.github.kxyr0.echoplayer.api.mutation.PlayerMutationId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Durable idempotency ledger for server-authoritative mutations.
 */
public final class PlayerMutationLedger {
    private final Map<PlayerMutationId, PlayerMutationReceipt> receipts = new LinkedHashMap<>();

    public synchronized PlayerMutationReceipt apply(PlayerMutationId id, String requestFingerprint,
                                                    Supplier<String> mutation) {
        PlayerMutationReceipt existing = receipts.get(id);
        if (existing != null && existing.requestFingerprint().equals(requestFingerprint)) {
            return new PlayerMutationReceipt(id, requestFingerprint, existing.result(), true, false);
        }
        if (existing != null) {
            return new PlayerMutationReceipt(id, requestFingerprint, existing.result(), true, true);
        }
        PlayerMutationReceipt receipt = new PlayerMutationReceipt(id, requestFingerprint, mutation.get(),
                false, false);
        receipts.put(id, receipt);
        return receipt;
    }

    public synchronized int size() {
        return receipts.size();
    }
}
