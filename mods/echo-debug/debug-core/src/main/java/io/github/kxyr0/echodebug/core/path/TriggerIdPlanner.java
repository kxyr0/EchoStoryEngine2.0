package io.github.kxyr0.echodebug.core.path;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.util.Locale;

public final class TriggerIdPlanner {
    private static final String ID_PATTERN = "[a-z0-9_.-]+:[a-z0-9_./-]+";
    private static final String PATH_PATTERN = "[a-z0-9_./-]+";

    public DebugOperationResult<TriggerIdPlan> plan(String namespace, String path) {
        String cleanNamespace = clean(namespace);
        String cleanPath = clean(path);
        if (!validParts(cleanNamespace, cleanPath)) {
            return DebugOperationResult.failure(DebugDiagnosticCode.DEBUG_TRIGGER_ID_INVALID,
                    "TriggerId must use lowercase namespace:path without traversal.", null);
        }
        TriggerId triggerId = new TriggerId(cleanNamespace + ":" + cleanPath);
        return DebugOperationResult.success(new TriggerIdPlan(triggerId, cleanNamespace, cleanPath,
                className(cleanPath)));
    }

    private static boolean validParts(String namespace, String path) {
        String id = namespace + ":" + path;
        return id.matches(ID_PATTERN) && path.matches(PATH_PATTERN) && !path.contains("..")
                && !path.startsWith("/") && !path.startsWith("\\") && !path.contains(":");
    }

    private static String className(String path) {
        String last = path.substring(path.lastIndexOf('/') + 1);
        StringBuilder builder = new StringBuilder();
        for (String token : last.split("[_.-]+")) {
            if (!token.isEmpty()) {
                builder.append(token.substring(0, 1).toUpperCase(Locale.ROOT)).append(token.substring(1));
            }
        }
        return builder.isEmpty() ? "GeneratedTrigger" : builder.toString();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
