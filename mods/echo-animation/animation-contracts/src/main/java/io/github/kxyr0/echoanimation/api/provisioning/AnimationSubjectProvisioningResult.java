package io.github.kxyr0.echoanimation.api.provisioning;

import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result of an ensure-subject operation.
 */
public record AnimationSubjectProvisioningResult(
        Status status,
        AnimationSubjectId subjectId,
        AnimationProfileId profileId,
        Optional<AnimationSubjectProvenance> provenance,
        String diagnosticCode,
        String message
) {
    public AnimationSubjectProvisioningResult {
        status = Objects.requireNonNull(status, "status");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        profileId = Objects.requireNonNull(profileId, "profileId");
        provenance = Objects.requireNonNull(provenance, "provenance");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnosticCode");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        CREATED,
        EXISTING,
        PROFILE_REBIND_REQUIRED,
        PROVENANCE_CONFLICT,
        PROFILE_NOT_FOUND,
        SERVICE_UNAVAILABLE
    }
}
