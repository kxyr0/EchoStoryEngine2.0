package io.github.kxyr0.echodebug.core.request;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import io.github.kxyr0.echodebug.core.identity.DebugRequestId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class TriggerWandRequestLedger<T> {
    private static final int MAX_ENTRIES = 128;
    private final Map<DebugRequestId, Entry<T>> entries = new LinkedHashMap<>();

    public DebugOperationResult<T> apply(
            DebugRequestId requestId,
            DebugOperationKind operation,
            String arguments,
            Supplier<DebugOperationResult<T>> action
    ) {
        Entry<T> existing = entries.get(Objects.requireNonNull(requestId, "requestId"));
        if (existing != null) {
            return replay(existing, operation, arguments);
        }
        DebugOperationResult<T> result = action.get();
        entries.put(requestId, new Entry<>(operation, arguments, result));
        trim();
        return result;
    }

    private DebugOperationResult<T> replay(Entry<T> entry, DebugOperationKind operation, String arguments) {
        if (entry.same(operation, arguments)) {
            return entry.result();
        }
        return DebugOperationResult.failure(DebugDiagnosticCode.DEBUG_REQUEST_IDEMPOTENCY_CONFLICT,
                "Request id was reused with different operation arguments.", entry.result().value().orElse(null));
    }

    private void trim() {
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.keySet().iterator().next());
        }
    }

    private record Entry<T>(DebugOperationKind operation, String arguments, DebugOperationResult<T> result) {
        Entry {
            operation = Objects.requireNonNull(operation, "operation");
            arguments = Objects.requireNonNull(arguments, "arguments");
            result = Objects.requireNonNull(result, "result");
        }

        boolean same(DebugOperationKind checkedOperation, String checkedArguments) {
            return operation == checkedOperation && arguments.equals(checkedArguments);
        }
    }
}
