package io.github.kxyr0.echotrigger.core.runtime;

import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.model.TriggerResult;
import io.github.kxyr0.echotrigger.api.status.TriggerResultCode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

final class TriggerCommandLedger {
    private final Map<TriggerCommandId, Entry> entries = new LinkedHashMap<>();

    TriggerResult apply(TriggerCommandId commandId, String signature, Supplier<TriggerResult> action) {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(signature, "signature");
        Entry previous = entries.get(commandId);
        if (previous != null && previous.signature().equals(signature)) {
            return previous.result();
        }
        if (previous != null) {
            return TriggerResult.rejected(TriggerResultCode.IDEMPOTENCY_CONFLICT,
                    "Command ID was reused with different arguments.");
        }
        TriggerResult result = action.get();
        entries.put(commandId, new Entry(signature, result));
        return result;
    }

    void clear() {
        entries.clear();
    }

    private record Entry(String signature, TriggerResult result) {
    }
}
