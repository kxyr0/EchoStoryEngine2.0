package io.github.kxyr0.echoscene.api.descriptor;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

public record SceneReference(SceneReferenceKind kind, String target, SourceSpan span) {
    public SceneReference {
        kind = Objects.requireNonNull(kind, "kind");
        target = Objects.requireNonNull(target, "target");
        span = Objects.requireNonNull(span, "span");
    }
}
