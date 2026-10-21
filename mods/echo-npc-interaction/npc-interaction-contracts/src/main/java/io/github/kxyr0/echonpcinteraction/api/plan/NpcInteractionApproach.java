package io.github.kxyr0.echonpcinteraction.api.plan;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Objects;

public record NpcInteractionApproach(
        Vector3 standingPosition,
        String facingDirection,
        NpcInteractionHit hit,
        double requiredReach,
        double clearance,
        boolean lineOfSightRequired,
        double estimatedApproachCost) {
    public NpcInteractionApproach {
        standingPosition = Objects.requireNonNull(standingPosition, "standingPosition");
        facingDirection = Objects.requireNonNull(facingDirection, "facingDirection").trim();
        hit = Objects.requireNonNull(hit, "hit");
        requireFinite(requiredReach, "requiredReach");
        requireFinite(clearance, "clearance");
        requireFinite(estimatedApproachCost, "estimatedApproachCost");
        if (facingDirection.isEmpty() || requiredReach <= 0D || clearance < 0D || estimatedApproachCost < 0D) {
            throw new IllegalArgumentException("approach values are outside supported bounds.");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
