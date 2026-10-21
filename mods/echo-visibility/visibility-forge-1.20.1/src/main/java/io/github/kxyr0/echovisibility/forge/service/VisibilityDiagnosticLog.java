package io.github.kxyr0.echovisibility.forge.service;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class VisibilityDiagnosticLog {
    private static final int CAPACITY = 256;
    private final Deque<VisibilityDiagnostic> diagnostics = new ArrayDeque<>();

    public void add(VisibilityDiagnostic diagnostic) {
        if (diagnostics.size() == CAPACITY) {
            diagnostics.removeFirst();
        }
        diagnostics.addLast(diagnostic);
    }

    public void addAll(List<VisibilityDiagnostic> values) {
        values.forEach(this::add);
    }

    public List<VisibilityDiagnostic> recent(Optional<VisibilityWatchId> watchId, int limit) {
        int bounded = Math.max(1, Math.min(limit, CAPACITY));
        List<VisibilityDiagnostic> result = new ArrayList<>();
        var iterator = diagnostics.descendingIterator();
        while (iterator.hasNext() && result.size() < bounded) {
            VisibilityDiagnostic diagnostic = iterator.next();
            if (watchId.isEmpty() || diagnostic.watchId().filter(watchId.orElseThrow()::equals).isPresent()) {
                result.add(diagnostic);
            }
        }
        return List.copyOf(result);
    }
}
