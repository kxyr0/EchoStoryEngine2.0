package io.github.kxyr0.echoevents.core.runtime;

import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.delivery.EventDelivery;
import io.github.kxyr0.echoevents.api.delivery.EventReactionPort;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionIndexRevision;
import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import io.github.kxyr0.echoevents.core.index.EventSubscriptionIndex;
import io.github.kxyr0.echoevents.core.journal.EventJournal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/** One server thread owns creation, reload, and dispatch. Publish never invokes reactions. */
public final class EventDispatcher {
    private final Thread owner = Thread.currentThread();
    private final EventJournal journal;
    private final EventReactionPort reactions;
    private final ArrayDeque<EventEnvelope<?>> history = new ArrayDeque<>();
    private final PriorityQueue<EventDelivery> pending = new PriorityQueue<>(java.util.Comparator
            .comparingLong(EventDelivery::nextTick).thenComparing(value -> value.event().source().gameTick())
            .thenComparing(value -> value.event().id().value()).thenComparing(value -> value.subscription().id()));
    private final int historyLimit;
    private final int transientLimit;
    private EventSubscriptionIndex index = new EventSubscriptionIndex(new EventSubscriptionIndexRevision(0), List.of());
    private boolean dispatching;
    private long sequence;
    private int transientPending;
    private long transientDropped;
    private final TransientCausalCounts transientCausal = new TransientCausalCounts();

    public EventDispatcher(EventJournal journal, EventReactionPort reactions, int historyLimit, int transientLimit) {
        if (historyLimit < 1 || transientLimit < 1) throw new IllegalArgumentException("Invalid event capacity");
        this.journal = java.util.Objects.requireNonNull(journal, "journal");
        this.reactions = java.util.Objects.requireNonNull(reactions, "reactions");
        this.historyLimit = historyLimit;
        this.transientLimit = transientLimit;
        journal.image().pending().forEach(value -> pending.add(new EventDelivery(value.event(), value.subscription(),
                value.attempts(), 0, value.status(), value.diagnostic())));
    }
    public void replaceSubscriptions(List<EventSubscription> subscriptions) {
        checkThread();
        var replacement = new EventSubscriptionIndex(new EventSubscriptionIndexRevision(sequence + 1), subscriptions);
        index = replacement;
        sequence++;
    }
    public io.github.kxyr0.echoevents.api.event.EventPublication prepare(EventEnvelope<?> event) {
        checkThread();
        return new io.github.kxyr0.echoevents.api.event.EventPublication(event,
                index.candidates(event).stream().filter(value -> value.filter().matches(event)).toList());
    }
    public boolean publish(EventEnvelope<?> event, EventMutationPolicy policy) {
        return publish(prepare(event), policy);
    }
    public boolean publish(io.github.kxyr0.echoevents.api.event.EventPublication publication, EventMutationPolicy policy) {
        var event = publication.event();
        checkThread();
        if (policy != EventMutationPolicy.NORMAL) return false;
        if (event.kind().durable() && journal.find(event.id()).isPresent()) {
            if (!journal.find(event.id()).get().sameTransition(event)) throw new IllegalArgumentException("Conflicting occurrence identity");
            return false;
        }
        List<EventDelivery> eligible = new ArrayList<>();
        for (var subscription : publication.subscriptions()) {
            if (subscription.filter().matches(event)) eligible.add(new EventDelivery(event, subscription, 0, 0,
                    EventDelivery.Status.PENDING, ""));
        }
        if (event.kind().durable()) journal.append(event, eligible);
        else if (transientPending + eligible.size() > transientLimit) {
            transientDropped++;
            return false;
        } else transientPending += eligible.size();
        pending.addAll(eligible);
        history.addLast(event);
        while (history.size() > historyLimit) history.removeFirst();
        return true;
    }
    public int tick(long tick, int budget) {
        checkThread();
        if (tick < 0 || budget < 1) throw new IllegalArgumentException("Invalid dispatch budget/tick");
        if (dispatching) throw new IllegalStateException("Reentrant event dispatch");
        dispatching = true;
        int processed = 0;
        try {
            while (processed < budget && !pending.isEmpty() && pending.peek().nextTick() <= tick) {
                EventDelivery delivery = pending.remove();
                try { process(delivery, tick); }
                catch (RuntimeException failure) { pending.add(delivery); throw failure; }
                if (!delivery.event().kind().durable()) transientPending--;
                processed++;
            }
            return processed;
        } finally { dispatching = false; }
    }
    public EventSubscriptionIndex index() { checkThread(); return index; }
    public List<EventEnvelope<?>> history() { checkThread(); return List.copyOf(history); }
    public int pendingCount() { checkThread(); return pending.size(); }
    public long transientDropped() { checkThread(); return transientDropped; }
    public io.github.kxyr0.echoevents.core.journal.EventJournalImage journalImage() { checkThread(); return journal.image(); }
    public void rememberContexts(java.util.Map<io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId,
            io.github.kxyr0.echoevents.api.context.EventExecutionContext> contexts) {
        checkThread();
        journal.rememberContexts(contexts);
    }
    public Optional<io.github.kxyr0.echoevents.api.context.EventExecutionContext> executionContext(
            io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId execution) {
        checkThread();
        return Optional.ofNullable(journal.image().executionContexts().get(execution));
    }

    private void process(EventDelivery delivery, long tick) {
        String once = delivery.subscription().id().value() + "|" + audienceKey(delivery.event());
        String causal = delivery.event().source().rootExecution().value() + "|"
                + delivery.event().kind().stableId() + "|" + delivery.event().source().resource().value()
                + "|" + delivery.subscription().id().value();
        boolean alreadyOnce = delivery.subscription().once() && journal.image().onceReceipts().contains(once);
        if (alreadyOnce) { finish(delivery, EventDelivery.Status.ACCEPTED, "ONCE_ALREADY_ACCEPTED", tick); return; }
        if (journal.image().causalCounts().getOrDefault(causal, 0) + transientCausal.get(causal) >= 32) {
            finish(delivery, EventDelivery.Status.FAILED, "ECHO_EVENT_CAUSAL_LIMIT", tick); return;
        }
        EventReactionPort.Acceptance result;
        try { result = java.util.Objects.requireNonNull(reactions.accept(delivery), "acceptance"); }
        catch (RuntimeException failure) { result = EventReactionPort.Acceptance.RETRY; }
        if (result == EventReactionPort.Acceptance.ACCEPTED) {
            var next = next(delivery, EventDelivery.Status.ACCEPTED, "", tick);
            if (delivery.event().kind().durable()) journal.update(next,
                    delivery.subscription().once() ? Optional.of(once) : Optional.empty(), Optional.of(causal));
            else {
                transientCausal.accepted(causal);
                if (delivery.subscription().once()) journal.markOnce(once);
            }
        } else if (result == EventReactionPort.Acceptance.WAITING_CONTEXT && delivery.event().kind().durable()) {
            var waiting = new EventDelivery(delivery.event(), delivery.subscription(), delivery.attempts(), tick + 200,
                    EventDelivery.Status.PENDING, "ECHO_EVENT_WAITING_CONTEXT");
            journal.update(waiting, Optional.empty(), Optional.empty());
            pending.add(waiting);
        } else if (result == EventReactionPort.Acceptance.RETRY && delivery.attempts() < 4) {
            var retry = next(delivery, EventDelivery.Status.PENDING, "ECHO_EVENT_RETRY", tick + (20L << delivery.attempts()));
            if (delivery.event().kind().durable()) journal.update(retry, Optional.empty(), Optional.empty());
            else transientPending++;
            pending.add(retry);
        } else finish(delivery, EventDelivery.Status.FAILED, "ECHO_EVENT_DELIVERY_FAILED", tick);
    }
    private void finish(EventDelivery delivery, EventDelivery.Status status, String diagnostic, long tick) {
        if (delivery.event().kind().durable()) journal.update(next(delivery, status, diagnostic, tick),
                Optional.empty(), Optional.empty());
    }
    private static EventDelivery next(EventDelivery delivery, EventDelivery.Status status, String diagnostic, long tick) {
        return new EventDelivery(delivery.event(), delivery.subscription(), delivery.attempts() + 1, tick, status, diagnostic);
    }
    private static String audienceKey(EventEnvelope<?> event) {
        return event.source().audience() instanceof EventAudience.Player player ? player.playerId().toString() : "WORLD";
    }
    private void checkThread() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Events require their authoritative server thread");
    }
}
