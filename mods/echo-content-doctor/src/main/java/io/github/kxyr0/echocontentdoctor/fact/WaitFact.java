package io.github.kxyr0.echocontentdoctor.fact;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;
import java.util.Optional;

public record WaitFact(
        String description,
        boolean clockPaused,
        boolean resumeReachable,
        Optional<String> heldCapability,
        Optional<String> childRequiredCapability,
        SourceSpan span
) {
    public WaitFact {
        description = Objects.requireNonNull(description, "description");
        heldCapability = Objects.requireNonNull(heldCapability, "heldCapability");
        childRequiredCapability = Objects.requireNonNull(childRequiredCapability, "childRequiredCapability");
        span = Objects.requireNonNull(span, "span");
    }
}
