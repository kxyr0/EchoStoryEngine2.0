package io.github.kxyr0.echomagic.core.cast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SpellBurstScheduler {
    private final int maximumEmissionsPerTick;

    public SpellBurstScheduler(int maximumEmissionsPerTick) {
        if (maximumEmissionsPerTick < 1) throw new IllegalArgumentException("Emission limit must be positive.");
        this.maximumEmissionsPerTick = maximumEmissionsPerTick;
    }

    public List<SpellCastInstance> due(Collection<SpellCastInstance> casts, long tick) {
        List<SpellCastInstance> result = new ArrayList<>();
        for (SpellCastInstance cast : casts) {
            if (result.size() == maximumEmissionsPerTick) break;
            if (cast.emissionDue(tick)) result.add(cast);
        }
        return List.copyOf(result);
    }
}

