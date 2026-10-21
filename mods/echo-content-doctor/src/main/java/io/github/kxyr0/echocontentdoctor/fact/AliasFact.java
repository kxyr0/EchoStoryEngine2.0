package io.github.kxyr0.echocontentdoctor.fact;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import java.util.Objects;

public record AliasFact(ContentResourceId oldId, ContentResourceId target, SourceSpan span) {
    public AliasFact {
        oldId = Objects.requireNonNull(oldId, "oldId");
        target = Objects.requireNonNull(target, "target");
        span = Objects.requireNonNull(span, "span");
    }
}
