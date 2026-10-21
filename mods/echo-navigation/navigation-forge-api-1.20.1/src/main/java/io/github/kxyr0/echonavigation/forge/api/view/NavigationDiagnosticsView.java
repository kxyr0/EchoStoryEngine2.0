package io.github.kxyr0.echonavigation.forge.api.view;

import io.github.kxyr0.echonavigation.api.diagnostic.NavigationDiagnostic;
import java.util.List;

public interface NavigationDiagnosticsView {
    List<NavigationDiagnostic> recent(int limit);
}
