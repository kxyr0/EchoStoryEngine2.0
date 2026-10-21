package io.github.kxyr0.echoanimation.api.provisioning;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Objects;

/**
 * Idempotent server-thread subject provisioning request.
 */
public record AnimationSubjectProvisioningRequest(
        AnimationSubjectId subjectId,
        AnimationProfileId profileId,
        AnimationSubjectProvenance provenance,
        AnimationCommandId commandId
) {
    public AnimationSubjectProvisioningRequest {
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        profileId = Objects.requireNonNull(profileId, "profileId");
        provenance = Objects.requireNonNull(provenance, "provenance");
        commandId = Objects.requireNonNull(commandId, "commandId");
    }
}
