package io.github.kxyr0.echoanimation.core.runtime;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationLeaseId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationGraphDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationTransitionDefinition;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic per-subject runtime state pinned to one graph revision.
 */
public final class AnimationSubjectRuntime {
    private final AnimationSubjectId subjectId;
    private final AnimationGraphDefinition graph;
    private final Map<AnimationChannelId, AnimationChannelSlot> channels = new HashMap<>();
    private final Map<AnimationChannelId, AnimationLease> leases = new HashMap<>();
    private long nextLeaseNumber = 1L;

    public AnimationSubjectRuntime(AnimationSubjectId subjectId, AnimationGraphDefinition graph) {
        this.subjectId = Objects.requireNonNull(subjectId, "subjectId");
        this.graph = Objects.requireNonNull(graph, "graph");
        graph.channels().forEach(channel -> channels.put(channel.id(),
                new AnimationChannelSlot(channel.id(), graph.state(channel.initialStateId()).orElseThrow())));
    }

    public AnimationLeaseResult acquire(AnimationChannelId channelId, AnimationOwnerId ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        if (!channels.containsKey(channelId)) {
            return new AnimationLeaseResult(AnimationLeaseResult.Status.UNKNOWN_CHANNEL, Optional.empty());
        }
        AnimationLease current = leases.get(channelId);
        if (current != null && !current.ownerId().equals(ownerId)) {
            return new AnimationLeaseResult(AnimationLeaseResult.Status.CONFLICT, Optional.empty());
        }
        AnimationLease lease = current == null ? newLease(channelId, ownerId) : current;
        leases.put(channelId, lease);
        return new AnimationLeaseResult(AnimationLeaseResult.Status.ACQUIRED, Optional.of(lease));
    }

    public void release(AnimationLease lease) {
        Objects.requireNonNull(lease, "lease");
        leases.remove(lease.channelId(), lease);
    }

    public AnimationStateChangeResult requestState(AnimationLease lease, AnimationStateId stateId) {
        Validation validation = validateLease(lease);
        if (validation.failed()) {
            return result(validation.status(), lease.channelId());
        }
        Optional<AnimationStateDefinition> target = graph.state(stateId)
                .filter(state -> state.channelId().equals(lease.channelId()));
        if (target.isEmpty()) {
            return result(AnimationStateChangeResult.Status.UNKNOWN_STATE, lease.channelId());
        }
        validation.slot().enter(target.orElseThrow());
        return result(AnimationStateChangeResult.Status.APPLIED, lease.channelId());
    }

    public AnimationStateChangeResult signal(AnimationLease lease, String eventId) {
        Validation validation = validateLease(lease);
        if (validation.failed()) {
            return result(validation.status(), lease.channelId());
        }
        AnimationChannelSlot slot = validation.slot();
        List<AnimationTransitionDefinition> matches = graph.transitionsFor(
                lease.channelId(), slot.state().id(), Objects.requireNonNull(eventId, "eventId"));
        if (matches.isEmpty()) {
            return result(AnimationStateChangeResult.Status.NO_MATCH, lease.channelId());
        }
        if (ambiguous(matches)) {
            return result(AnimationStateChangeResult.Status.AMBIGUOUS_TRANSITION, lease.channelId());
        }
        AnimationTransitionDefinition transition = matches.get(0);
        slot.enter(graph.state(transition.toStateId()).orElseThrow(), transition.blendDurationTicks());
        return result(AnimationStateChangeResult.Status.APPLIED, lease.channelId());
    }

    public void tick(long logicalTicks) {
        channels.values().forEach(channel -> channel.tick(logicalTicks));
    }

    public AnimationSubjectSnapshot snapshot() {
        Map<AnimationChannelId, AnimationSubjectSnapshot.ChannelView> views = new HashMap<>();
        channels.forEach((channelId, slot) -> views.put(channelId, slot.view()));
        return new AnimationSubjectSnapshot(subjectId, graph.revision(), views);
    }

    private AnimationLease newLease(AnimationChannelId channelId, AnimationOwnerId ownerId) {
        AnimationLeaseId leaseId = new AnimationLeaseId("lease-" + nextLeaseNumber);
        nextLeaseNumber++;
        return new AnimationLease(leaseId, ownerId, channelId);
    }

    private Validation validateLease(AnimationLease lease) {
        Objects.requireNonNull(lease, "lease");
        AnimationChannelSlot slot = channels.get(lease.channelId());
        if (slot == null) {
            return Validation.failed(AnimationStateChangeResult.Status.UNKNOWN_CHANNEL);
        }
        AnimationLease current = leases.get(lease.channelId());
        if (current == null) {
            return Validation.failed(AnimationStateChangeResult.Status.LEASE_RELEASED);
        }
        if (!current.equals(lease)) {
            return Validation.failed(AnimationStateChangeResult.Status.CONFLICT);
        }
        return Validation.valid(slot);
    }

    private AnimationStateChangeResult result(
            AnimationStateChangeResult.Status status,
            AnimationChannelId channelId
    ) {
        AnimationChannelSlot slot = channels.get(channelId);
        Optional<AnimationStateId> stateId = slot == null ? Optional.empty() : Optional.of(slot.state().id());
        return new AnimationStateChangeResult(status, channelId, stateId);
    }

    private static boolean ambiguous(List<AnimationTransitionDefinition> matches) {
        return matches.size() > 1 && matches.get(0).order() == matches.get(1).order();
    }

    private record Validation(AnimationStateChangeResult.Status status, AnimationChannelSlot slot) {
        static Validation valid(AnimationChannelSlot slot) {
            return new Validation(null, slot);
        }

        static Validation failed(AnimationStateChangeResult.Status status) {
            return new Validation(status, null);
        }

        boolean failed() {
            return status != null;
        }
    }
}
