package io.github.kxyr0.echotime.core.mutation;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TimeMutationLedger {
    private final Map<TimeMutationId, TimeMutationReceipt> receipts = new LinkedHashMap<>();

    public Optional<TimeMutationReceipt> find(TimeMutationId id) {
        return Optional.ofNullable(receipts.get(Objects.requireNonNull(id, "id")));
    }

    public void record(TimeMutationReceipt receipt) {
        Objects.requireNonNull(receipt, "receipt");
        TimeMutationReceipt previous = receipts.putIfAbsent(receipt.mutationId(), receipt);
        if (previous != null && !previous.equals(receipt)) {
            throw new IllegalStateException("Time mutation receipt conflict for " + receipt.mutationId().value());
        }
    }

    public List<TimeMutationReceipt> receipts() {
        return List.copyOf(receipts.values());
    }

    public void restore(List<TimeMutationReceipt> restored) {
        receipts.clear();
        Objects.requireNonNull(restored, "restored").forEach(this::record);
    }
}
