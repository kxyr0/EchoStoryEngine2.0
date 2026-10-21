package io.github.kxyr0.echoevents.api.delivery;

/** Implementations must durably deduplicate executionToken before returning ACCEPTED. */
@FunctionalInterface
public interface EventReactionPort {
    Acceptance accept(EventDelivery delivery);

    enum Acceptance { ACCEPTED, RETRY, WAITING_CONTEXT, REJECTED }
}
