package io.github.kxyr0.echobackgroundnpc.core.diagnostic;

import io.github.kxyr0.echobackgroundnpc.api.diagnostic.BackgroundDiagnostic;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Bounded transition/error log; steady-state ticks produce no messages. */
public final class BackgroundDiagnosticLog {
    private final int capacity;
    private final Deque<BackgroundDiagnostic> diagnostics = new ArrayDeque<>();

    public BackgroundDiagnosticLog(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive.");
        this.capacity = capacity;
    }
    public synchronized void accept(BackgroundDiagnostic value) {
        diagnostics.addLast(value);
        while (diagnostics.size() > capacity) diagnostics.removeFirst();
    }
    public synchronized List<BackgroundDiagnostic> recent(int limit) {
        List<BackgroundDiagnostic> values = new ArrayList<>(diagnostics);
        int from = Math.max(0, values.size() - Math.max(0, limit));
        return List.copyOf(values.subList(from, values.size()));
    }
}
