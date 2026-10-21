package io.github.kxyr0.echoeffects.forge.api;

import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import java.util.List;

public interface EffectQueryService {
    List<ActiveEffectSnapshot> activeEffects(EffectTargetRef target);

    boolean isActive(EffectTargetRef target, EffectId effectId);
}
