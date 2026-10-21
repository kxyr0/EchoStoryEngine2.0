package io.github.kxyr0.echonpcinteraction.forge.runtime;

import io.github.kxyr0.echonpcinteraction.api.diagnostic.NpcInteractionDiagnostic;
import io.github.kxyr0.echonpcinteraction.forge.api.view.NpcInteractionDiagnosticsView;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

public final class InteractionDiagnosticsLog {
    private final ArrayDeque<NpcInteractionDiagnostic> diagnostics = new ArrayDeque<>();
    private long revision;

    public void add(String code, String source, String messageRu, long tick, Map<String, String> context) {
        diagnostics.addLast(new NpcInteractionDiagnostic(code, source, messageRu, "WARN", tick, context));
        while (diagnostics.size() > 200) {
            diagnostics.removeFirst();
        }
        revision++;
    }

    public NpcInteractionDiagnosticsView view() {
        return new NpcInteractionDiagnosticsView(revision, List.copyOf(diagnostics));
    }
}
