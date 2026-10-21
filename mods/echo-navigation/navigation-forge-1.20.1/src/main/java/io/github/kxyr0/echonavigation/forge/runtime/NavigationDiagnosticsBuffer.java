package io.github.kxyr0.echonavigation.forge.runtime;

import io.github.kxyr0.echonavigation.api.diagnostic.NavigationDiagnostic;
import io.github.kxyr0.echonavigation.forge.api.view.NavigationDiagnosticsView;
import java.util.ArrayList;
import java.util.List;

final class NavigationDiagnosticsBuffer {
    private static final int MAX_ENTRIES = 200;
    private final List<NavigationDiagnostic> entries = new ArrayList<>();

    void add(NavigationDiagnostic diagnostic) {
        entries.add(diagnostic);
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
    }

    NavigationDiagnosticsView view() {
        return limit -> entries.stream().skip(Math.max(0, entries.size() - limit)).toList();
    }
}
