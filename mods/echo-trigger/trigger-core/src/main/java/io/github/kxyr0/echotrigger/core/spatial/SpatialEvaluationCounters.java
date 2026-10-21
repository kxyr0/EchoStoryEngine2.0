package io.github.kxyr0.echotrigger.core.spatial;

public record SpatialEvaluationCounters(
        long playersSampled,
        long candidateAreasEvaluated,
        long membershipComparisons,
        long fullCatalogScans,
        long enterEvents,
        long exitEvents
) {
    public SpatialEvaluationCounters plus(SpatialEvaluationCounters other) {
        return new SpatialEvaluationCounters(
                playersSampled + other.playersSampled(),
                candidateAreasEvaluated + other.candidateAreasEvaluated(),
                membershipComparisons + other.membershipComparisons(),
                fullCatalogScans + other.fullCatalogScans(),
                enterEvents + other.enterEvents(),
                exitEvents + other.exitEvents());
    }
}
