package io.github.kxyr0.echonpc.forge.diagnostic;

import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnosticSeverity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bounded lifecycle-owned diagnostic buffer for command output.
 */
public final class InMemoryDiagnosticBuffer {
    private final int capacity;
    private final ArrayDeque<NpcDiagnostic> diagnostics = new ArrayDeque<>();

    public InMemoryDiagnosticBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive.");
        }
        this.capacity = capacity;
    }

    public void add(NpcDiagnostic diagnostic) {
        diagnostics.addFirst(Objects.requireNonNull(diagnostic, "diagnostic"));
        while (diagnostics.size() > capacity) {
            diagnostics.removeLast();
        }
    }

    public List<NpcDiagnostic> newest(int limit) {
        int bounded = Math.max(0, Math.min(limit, capacity));
        return new ArrayList<>(diagnostics).stream().limit(bounded).toList();
    }

    public boolean hasWarnings() {
        return diagnostics.stream().anyMatch(item -> item.severity() == NpcDiagnosticSeverity.WARNING);
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(NpcDiagnostic::error);
    }
}
