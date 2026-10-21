package io.github.kxyr0.echonpcbackpack.animation;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;

public record NpcBackpackAnimationBinding(ActorId actorId, String formId,
        AnimationId equipAnimation, AnimationId unequipAnimation) {
    public NpcBackpackAnimationBinding {
        actorId = Objects.requireNonNull(actorId, "actorId");
        formId = Objects.requireNonNull(formId, "formId").trim();
        equipAnimation = Objects.requireNonNull(equipAnimation, "equipAnimation");
        unequipAnimation = Objects.requireNonNull(unequipAnimation, "unequipAnimation");
        if (formId.isEmpty()) throw new IllegalArgumentException("formId must be non-empty or '*'.");
    }
}
