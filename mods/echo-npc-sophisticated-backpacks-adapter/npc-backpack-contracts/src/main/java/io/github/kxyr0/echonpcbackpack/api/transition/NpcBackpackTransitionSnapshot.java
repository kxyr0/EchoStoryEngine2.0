package io.github.kxyr0.echonpcbackpack.api.transition;

import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackTransitionId;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import java.util.Objects;
import java.util.Optional;

public record NpcBackpackTransitionSnapshot(
        NpcBackpackTransitionId id,
        NpcBackpackTransitionType type,
        NpcBackpackTransitionPhase phase,
        NpcBackpackTransitionPolicy policy,
        Optional<BackpackBlockLocation> source,
        Optional<BackpackBlockLocation> destination,
        long revision
) {
    public NpcBackpackTransitionSnapshot {
        id = Objects.requireNonNull(id, "id");
        type = Objects.requireNonNull(type, "type");
        phase = Objects.requireNonNull(phase, "phase");
        policy = Objects.requireNonNull(policy, "policy");
        source = Objects.requireNonNull(source, "source");
        destination = Objects.requireNonNull(destination, "destination");
        if (revision < 1L) {
            throw new IllegalArgumentException("revision must be positive.");
        }
    }
}
