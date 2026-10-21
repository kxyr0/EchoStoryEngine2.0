package io.github.kxyr0.echobackgroundnpc.echoscript;

import io.github.kxyr0.echobackgroundnpc.echoscript.binding.BackgroundResourceContext;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import java.util.Objects;

record ParsedBackgroundScript(BackgroundResourceContext context, CompilationUnitSyntax unit) {
    ParsedBackgroundScript {
        context = Objects.requireNonNull(context, "context");
        unit = Objects.requireNonNull(unit, "unit");
    }
}
