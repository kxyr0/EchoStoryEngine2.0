package io.github.kxyr0.echodebug.forge.creation;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class TriggerCreationLedger {
    private static final int MAX_ENTRIES = 128;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public TriggerCreationResult apply(String requestId, String arguments, Supplier<TriggerCreationResult> action) {
        Entry existing = entries.get(Objects.requireNonNull(requestId, "requestId"));
        if (existing != null) {
            return existing.arguments().equals(arguments) ? existing.result() : conflict();
        }
        TriggerCreationResult result = action.get();
        entries.put(requestId, new Entry(arguments, result));
        trim();
        return result;
    }

    private TriggerCreationResult conflict() {
        return TriggerCreationResult.failure(DebugDiagnosticCode.DEBUG_REQUEST_IDEMPOTENCY_CONFLICT,
                "Creation request id was reused with different arguments.");
    }

    private void trim() {
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.keySet().iterator().next());
        }
    }

    private record Entry(String arguments, TriggerCreationResult result) {
    }
}
