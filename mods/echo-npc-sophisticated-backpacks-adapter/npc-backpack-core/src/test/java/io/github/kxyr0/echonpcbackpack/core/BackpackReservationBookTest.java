package io.github.kxyr0.echonpcbackpack.core;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackReservationId;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.api.world.DimensionId;
import io.github.kxyr0.echonpcbackpack.core.reservation.BackpackReservationBook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class BackpackReservationBookTest {
    @Test
    void exactWorldPositionHasOneOwner() {
        BackpackReservationBook book = new BackpackReservationBook();
        BackpackBlockLocation location = new BackpackBlockLocation(new DimensionId("minecraft:overworld"),
                new BlockPosition(1, 2, 3));
        Assertions.assertTrue(book.acquire(new NpcBackpackReservationId("test:first"),
                new ActorId("jdh:first"), location, 1L).isPresent());
        Assertions.assertTrue(book.acquire(new NpcBackpackReservationId("test:second"),
                new ActorId("jdh:second"), location, 2L).isEmpty());
    }
}
