package io.github.kxyr0.echothoughtspace.forge.client.render;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import java.util.HashMap;
import java.util.Map;

public final class ThoughtHoverAnimation {
    private final Map<ThoughtNodeId, Double> values = new HashMap<>();

    public double update(ThoughtNodeId id, boolean hovered, double deltaSeconds) {
        double current = values.getOrDefault(id, 0.0D);
        double target = hovered ? 1.0D : 0.0D;
        double blend = 1.0D - Math.exp(-deltaSeconds / 0.10D);
        current += (target - current) * blend;
        if (Math.abs(current - target) < 0.001D) current = target;
        values.put(id, current);
        return current;
    }
}
