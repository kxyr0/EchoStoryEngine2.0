package io.github.kxyr0.echonpcanimationadapter;

import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;

/**
 * Maps NPC logical identity to neutral animation subject identity.
 */
public final class AnimationSubjectMapping {
    public AnimationSubjectId subjectId(ActorId actorId) {
        return new AnimationSubjectId(Objects.requireNonNull(actorId, "actorId").value());
    }
}
