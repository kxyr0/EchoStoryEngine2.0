package io.github.kxyr0.echocontentdoctor.model;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

public record ContentReference(ContentReferenceKind kind, ContentResourceId target, SourceSpan span) {
    public ContentReference {
        kind = Objects.requireNonNull(kind, "kind");
        target = Objects.requireNonNull(target, "target");
        span = Objects.requireNonNull(span, "span");
    }
}
