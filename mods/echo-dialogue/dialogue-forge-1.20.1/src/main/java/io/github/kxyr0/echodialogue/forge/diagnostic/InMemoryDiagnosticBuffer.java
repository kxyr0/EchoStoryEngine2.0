package io.github.kxyr0.echodialogue.forge.diagnostic;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Bounded in-memory diagnostic buffer exposed by commands.
 */
public final class InMemoryDiagnosticBuffer {
    private final int capacity;
    private final ArrayDeque<DialogueDiagnostic> diagnostics = new ArrayDeque<>();

    public InMemoryDiagnosticBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Diagnostic buffer capacity must be positive.");
        }
        this.capacity = capacity;
    }

    public void accept(DialogueDiagnostic diagnostic) {
        diagnostics.addLast(diagnostic);
        while (diagnostics.size() > capacity) {
            diagnostics.removeFirst();
        }
    }

    public List<DialogueDiagnostic> recent() {
        return List.copyOf(new ArrayList<>(diagnostics));
    }
}
