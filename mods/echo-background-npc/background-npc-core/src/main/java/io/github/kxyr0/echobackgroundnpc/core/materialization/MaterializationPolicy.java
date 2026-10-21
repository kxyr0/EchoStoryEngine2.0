package io.github.kxyr0.echobackgroundnpc.core.materialization;

import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundSimulationLod;
import io.github.kxyr0.echobackgroundnpc.api.runtime.MaterializationState;

/** Global distance thresholds with explicit hysteresis. */
public record MaterializationPolicy(
        double materializeDistance,
        double dematerializeDistance,
        double nearDistance,
        double activeDistance
) {
    public MaterializationPolicy {
        if (!(nearDistance > 0.0 && activeDistance > nearDistance
                && materializeDistance > activeDistance && dematerializeDistance > materializeDistance)) {
            throw new IllegalArgumentException("Materialization thresholds must be strictly increasing.");
        }
    }

    public static MaterializationPolicy defaults() { return new MaterializationPolicy(64.0, 76.0, 14.0, 40.0); }

    public Presence classify(MaterializationState current, double nearestPlayerDistance) {
        boolean materialized = current == MaterializationState.MATERIALIZED
                ? nearestPlayerDistance <= dematerializeDistance : nearestPlayerDistance <= materializeDistance;
        if (!materialized) return new Presence(MaterializationState.VIRTUAL, BackgroundSimulationLod.VIRTUAL);
        if (nearestPlayerDistance <= nearDistance) {
            return new Presence(MaterializationState.MATERIALIZED, BackgroundSimulationLod.NEAR);
        }
        if (nearestPlayerDistance <= activeDistance) {
            return new Presence(MaterializationState.MATERIALIZED, BackgroundSimulationLod.ACTIVE);
        }
        return new Presence(MaterializationState.MATERIALIZED, BackgroundSimulationLod.COARSE);
    }

    public record Presence(MaterializationState materialization, BackgroundSimulationLod lod) { }
}
