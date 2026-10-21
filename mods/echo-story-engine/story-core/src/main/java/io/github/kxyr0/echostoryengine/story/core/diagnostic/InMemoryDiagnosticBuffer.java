package io.github.kxyr0.echostoryengine.story.core.diagnostic;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

/**
 * Bounded in-memory diagnostic read model.
 *
 * <p>This is not a log file and does not write to disk. It stores only the most recent diagnostics so command or
 * visual adapters can read the same structured data without creating a separate diagnostic system.
 */
public final class InMemoryDiagnosticBuffer implements DiagnosticSink {
    private final int capacity;
    private final ArrayDeque<StoryDiagnostic> diagnostics;

    public InMemoryDiagnosticBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.diagnostics = new ArrayDeque<>(capacity);
    }

    @Override
    public synchronized void accept(StoryDiagnostic diagnostic) {
        diagnostics.addLast(Objects.requireNonNull(diagnostic, "diagnostic"));
        while (diagnostics.size() > capacity) {
            diagnostics.removeFirst();
        }
    }

    public synchronized List<StoryDiagnostic> recent() {
        return List.copyOf(diagnostics);
    }

    public synchronized List<StoryDiagnostic> recentBySeverity(DiagnosticSeverity severity) {
        Objects.requireNonNull(severity, "severity");
        return diagnostics.stream().filter(diagnostic -> diagnostic.severity() == severity).toList();
    }

    public synchronized List<StoryDiagnostic> recentByCode(String code) {
        String checked = Objects.requireNonNull(code, "code");
        return diagnostics.stream().filter(diagnostic -> diagnostic.code().equals(checked)).toList();
    }
}
