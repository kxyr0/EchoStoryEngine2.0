package io.github.kxyr0.echobackgroundnpc.core.persistence;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.AgentLifeState;
import java.util.Objects;

/** Only durable exceptions are saved; transient paths and interpolation are reconstructed. */
public record BackgroundPersistentState(
        BackgroundNpcId id,
        AgentLifeState life,
        long appearanceSeed,
        boolean durableStoryOverride
) {
    public BackgroundPersistentState {
        id = Objects.requireNonNull(id, "id");
        life = Objects.requireNonNull(life, "life");
    }
}
