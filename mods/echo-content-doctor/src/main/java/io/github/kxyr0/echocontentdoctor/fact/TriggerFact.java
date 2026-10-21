package io.github.kxyr0.echocontentdoctor.fact;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

public record TriggerFact(boolean providerExists, boolean selectorValid, boolean constantFalse, SourceSpan span) {
    public TriggerFact {
        span = Objects.requireNonNull(span, "span");
    }
}
