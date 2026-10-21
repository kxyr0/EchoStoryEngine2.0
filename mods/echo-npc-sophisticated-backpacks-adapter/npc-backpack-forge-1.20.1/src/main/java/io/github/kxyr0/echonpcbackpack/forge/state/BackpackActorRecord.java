package io.github.kxyr0.echonpcbackpack.forge.state;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackAttachmentId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackReservationId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackTransitionId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/**
 * Mutable server-thread record saved by the atomic journal.
 */
public final class BackpackActorRecord {
    public ActorId actorId;
    public NpcBackpackAttachmentId attachmentId;
    public ItemStack payload = ItemStack.EMPTY;
    public NpcBackpackAttachmentPoint attachmentPoint = NpcBackpackAttachmentPoint.NONE;
    public NpcBackpackStatus status = NpcBackpackStatus.NONE;
    public Optional<NpcBackpackTransitionId> transitionId = Optional.empty();
    public Optional<NpcBackpackCommandId> commandId = Optional.empty();
    public Optional<NpcBackpackReservationId> reservationId = Optional.empty();
    public Optional<NpcBackpackTransitionType> transitionType = Optional.empty();
    public Optional<NpcBackpackTransitionPhase> phase = Optional.empty();
    public NpcBackpackTransitionPolicy policy = NpcBackpackTransitionPolicy.IMMEDIATE;
    public Optional<BackpackBlockLocation> source = Optional.empty();
    public Optional<BackpackBlockLocation> destination = Optional.empty();
    public Optional<String> expectedFingerprint = Optional.empty();
    public Optional<String> navigationMoveId = Optional.empty();
    public final Set<String> markerReceipts = new LinkedHashSet<>();
    public String diagnosticCode = "ENPC-SB-OK";
    public String diagnosticMessage = "Ready.";
    public long revision = 1L;

    public boolean carryingPayload() {
        return !payload.isEmpty();
    }

    public boolean transitioning() {
        return phase.filter(value -> !value.terminal()).isPresent();
    }

    public void clearTransition() {
        transitionId = Optional.empty();
        commandId = Optional.empty();
        reservationId = Optional.empty();
        transitionType = Optional.empty();
        phase = Optional.empty();
        source = Optional.empty();
        destination = Optional.empty();
        navigationMoveId = Optional.empty();
        markerReceipts.clear();
    }
}
