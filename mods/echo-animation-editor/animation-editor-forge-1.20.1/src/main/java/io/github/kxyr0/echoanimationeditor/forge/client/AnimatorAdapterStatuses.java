package io.github.kxyr0.echoanimationeditor.forge.client;

import io.github.kxyr0.echoanimationeditor.api.diagnostic.AnimatorPublicApiStatus;
import java.util.Objects;

public record AnimatorAdapterStatuses(AnimatorPublicApiStatus animation, AnimatorPublicApiStatus npc) {
    public AnimatorAdapterStatuses {
        animation = Objects.requireNonNull(animation, "animation");
        npc = Objects.requireNonNull(npc, "npc");
    }
}
