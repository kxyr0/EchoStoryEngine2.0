package io.github.kxyr0.echoanimation.api.provisioning;

import java.util.Objects;

/**
 * Durable external ownership key for one animation subject.
 */
public record AnimationSubjectProvenance(
        AnimationSubjectLifecycleOwnerId lifecycleOwnerId,
        String externalCanonicalKey
) {
    public AnimationSubjectProvenance {
        lifecycleOwnerId = Objects.requireNonNull(lifecycleOwnerId, "lifecycleOwnerId");
        externalCanonicalKey = Objects.requireNonNull(externalCanonicalKey, "externalCanonicalKey").trim();
        if (externalCanonicalKey.isEmpty() || externalCanonicalKey.length() > 128) {
            throw new IllegalArgumentException("externalCanonicalKey must be non-empty and at most 128 chars.");
        }
    }
}
