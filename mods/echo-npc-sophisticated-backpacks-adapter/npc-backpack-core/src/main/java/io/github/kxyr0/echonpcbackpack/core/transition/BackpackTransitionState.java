package io.github.kxyr0.echonpcbackpack.core.transition;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackTransitionId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable server-thread state; Forge persists every destructive boundary.
 */
public final class BackpackTransitionState {
    public final ActorId actorId;
    public final NpcBackpackTransitionId id;
    public final NpcBackpackTransitionType type;
    public final NpcBackpackTransitionPolicy policy;
    public final Optional<BackpackBlockLocation> source;
    public final Optional<BackpackBlockLocation> destination;
    public final Set<String> markerReceipts = new LinkedHashSet<>();
    public NpcBackpackTransitionPhase phase = NpcBackpackTransitionPhase.RESERVED;
    public NpcBackpackAttachmentPoint attachmentPoint = NpcBackpackAttachmentPoint.NONE;
    public long revision = 1L;

    public BackpackTransitionState(ActorId actorId, NpcBackpackTransitionId id,
            NpcBackpackTransitionType type, NpcBackpackTransitionPolicy policy,
            Optional<BackpackBlockLocation> source, Optional<BackpackBlockLocation> destination) {
        this.actorId = Objects.requireNonNull(actorId, "actorId");
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.source = Objects.requireNonNull(source, "source");
        this.destination = Objects.requireNonNull(destination, "destination");
        if (type == NpcBackpackTransitionType.EQUIP && source.isEmpty()
                || type == NpcBackpackTransitionType.UNEQUIP && destination.isEmpty()) {
            throw new IllegalArgumentException("Transition location does not match type.");
        }
    }
}
