package io.github.kxyr0.echocontentdoctor.fact;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;
import java.util.Optional;

public record VoiceFact(boolean explicit, Optional<String> assetId, boolean supportedFormat,
        boolean metadataValid, SourceSpan span) {
    public VoiceFact {
        assetId = Objects.requireNonNull(assetId, "assetId");
        span = Objects.requireNonNull(span, "span");
    }
}
