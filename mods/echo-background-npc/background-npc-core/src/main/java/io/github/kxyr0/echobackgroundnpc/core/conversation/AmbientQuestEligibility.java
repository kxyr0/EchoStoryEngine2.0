package io.github.kxyr0.echobackgroundnpc.core.conversation;

/** Pure eligibility rule shared by the authoritative Forge completion path and tests. */
public final class AmbientQuestEligibility {
    public boolean eligible(boolean observedConversation, boolean sameDimension, boolean lineOfSight,
            double firstDistanceSquared, double secondDistanceSquared, double maximumDistance) {
        double limit = maximumDistance * maximumDistance;
        return observedConversation && sameDimension && lineOfSight
                && Math.min(firstDistanceSquared, secondDistanceSquared) <= limit;
    }
}
