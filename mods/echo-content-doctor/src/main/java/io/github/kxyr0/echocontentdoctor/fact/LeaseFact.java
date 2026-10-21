package io.github.kxyr0.echocontentdoctor.fact;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

public record LeaseFact(String capability, boolean automatic, boolean releasedOnAllTerminalPaths, SourceSpan span) {
    public LeaseFact {
        capability = Objects.requireNonNull(capability, "capability");
        span = Objects.requireNonNull(span, "span");
    }
}
