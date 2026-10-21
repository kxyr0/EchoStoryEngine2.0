package io.github.kxyr0.echodevscenarios.compile;

import io.github.kxyr0.echoscript.api.source.SourceSpan;

public record ScenarioDiagnostic(String code, String message, SourceSpan span) { }
