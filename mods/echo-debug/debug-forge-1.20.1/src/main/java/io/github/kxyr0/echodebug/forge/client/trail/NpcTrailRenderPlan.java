package io.github.kxyr0.echodebug.forge.client.trail;

import io.github.kxyr0.echodebug.core.trail.NpcMovementTrail;
import java.util.List;

public record NpcTrailRenderPlan(long revision, List<NpcMovementTrail> trails) {
    private static final NpcTrailRenderPlan EMPTY = new NpcTrailRenderPlan(0L, List.of());

    public NpcTrailRenderPlan {
        trails = List.copyOf(trails);
    }

    public static NpcTrailRenderPlan empty() {
        return EMPTY;
    }
}
