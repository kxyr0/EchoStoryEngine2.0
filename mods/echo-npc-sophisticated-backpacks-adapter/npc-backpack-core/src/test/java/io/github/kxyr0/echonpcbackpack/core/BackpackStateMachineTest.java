package io.github.kxyr0.echonpcbackpack.core;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackTransitionId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.api.world.DimensionId;
import io.github.kxyr0.echonpcbackpack.core.transition.BackpackMarkerSequence;
import io.github.kxyr0.echonpcbackpack.core.transition.BackpackStateMachine;
import io.github.kxyr0.echonpcbackpack.core.transition.BackpackTransitionDecision;
import io.github.kxyr0.echonpcbackpack.core.transition.BackpackTransitionState;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class BackpackStateMachineTest {
    @Test
    void equipMarkersAreOrderedAndDuplicateDeliveryIsIdempotent() {
        BackpackTransitionState state = state(NpcBackpackTransitionType.EQUIP);
        BackpackStateMachine machine = new BackpackStateMachine();

        Assertions.assertEquals(BackpackTransitionDecision.Status.OUT_OF_ORDER,
                machine.marker(state, BackpackMarkerSequence.BACK, "p1:1").status());
        Assertions.assertEquals(BackpackTransitionDecision.Status.ADVANCED,
                machine.marker(state, BackpackMarkerSequence.TAKE, "p1:2").status());
        Assertions.assertEquals(BackpackTransitionDecision.Status.ADVANCED,
                machine.marker(state, BackpackMarkerSequence.RIGHT_HAND, "p1:3").status());
        Assertions.assertEquals(NpcBackpackAttachmentPoint.RIGHT_HAND, state.attachmentPoint);
        Assertions.assertEquals(BackpackTransitionDecision.Status.DUPLICATE,
                machine.marker(state, BackpackMarkerSequence.RIGHT_HAND, "p1:3").status());
        Assertions.assertEquals(BackpackTransitionDecision.Status.ADVANCED,
                machine.marker(state, BackpackMarkerSequence.BACK, "p1:4").status());
        Assertions.assertEquals(NpcBackpackAttachmentPoint.BACK, state.attachmentPoint);
    }

    @Test
    void unequipCannotPlaceBeforeDetach() {
        BackpackTransitionState state = state(NpcBackpackTransitionType.UNEQUIP);
        state.attachmentPoint = NpcBackpackAttachmentPoint.BACK;
        BackpackStateMachine machine = new BackpackStateMachine();

        Assertions.assertEquals(BackpackTransitionDecision.Status.OUT_OF_ORDER,
                machine.marker(state, BackpackMarkerSequence.PLACE, "p2:1").status());
        Assertions.assertEquals(BackpackTransitionDecision.Status.ADVANCED,
                machine.marker(state, BackpackMarkerSequence.DETACH, "p2:2").status());
        Assertions.assertEquals(BackpackTransitionDecision.Status.ADVANCED,
                machine.marker(state, BackpackMarkerSequence.PLACE, "p2:3").status());
        Assertions.assertEquals(NpcBackpackAttachmentPoint.NONE, state.attachmentPoint);
    }

    private BackpackTransitionState state(NpcBackpackTransitionType type) {
        BackpackBlockLocation location = new BackpackBlockLocation(new DimensionId("minecraft:overworld"),
                new BlockPosition(1, 2, 3));
        return new BackpackTransitionState(new ActorId("jdh:molly"),
                new NpcBackpackTransitionId("test:transition"), type,
                NpcBackpackTransitionPolicy.ANIMATION_MARKERS,
                type == NpcBackpackTransitionType.EQUIP ? Optional.of(location) : Optional.empty(),
                type == NpcBackpackTransitionType.UNEQUIP ? Optional.of(location) : Optional.empty());
    }
}
