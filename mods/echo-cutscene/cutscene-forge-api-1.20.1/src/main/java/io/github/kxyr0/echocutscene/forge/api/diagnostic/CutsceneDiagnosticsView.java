package io.github.kxyr0.echocutscene.forge.api.diagnostic;

import io.github.kxyr0.echocutscene.api.runtime.CutsceneDiagnostic;
import java.util.List;

public interface CutsceneDiagnosticsView {
    List<CutsceneDiagnostic> recent();
}
