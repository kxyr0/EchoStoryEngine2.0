package io.github.kxyr0.echoeffects.forge.api;

import io.github.kxyr0.echoeffects.api.identity.EffectCommandId;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import java.util.Optional;

public interface EffectService {
    Optional<ActiveEffectSnapshot> applyToTarget(
            EffectTargetRef target,
            EffectId effectId,
            EffectCommandId commandId,
            double intensity
    );

    boolean removeFromTarget(EffectTargetRef target, EffectId effectId, long fadeOutTicks);
}
