package io.github.kxyr0.echoplayeractor.core.control;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoplayeractor.api.control.PlayerControlLease;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlMode;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlLeaseId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlScopeId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerControlLeaseRegistryTest {
    private final PlayerActorId actorId = PlayerActorId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private final PlayerActorOwnerId root = PlayerActorOwnerId.cutscene("playback-a");
    private final PlayerActorOwnerId dialogue = PlayerActorOwnerId.dialogue("dialogue-a");

    @Test
    void authorisesRootAndLineageOnly() {
        PlayerControlLeaseRegistry registry = new PlayerControlLeaseRegistry();
        registry.acquire(lease(root, dialogue));

        assertTrue(registry.authorises(actorId, root));
        assertTrue(registry.authorises(actorId, dialogue));
        assertFalse(registry.authorises(actorId, PlayerActorOwnerId.system("foreign")));
    }

    @Test
    void rejectsConflictingLease() {
        PlayerControlLeaseRegistry registry = new PlayerControlLeaseRegistry();
        registry.acquire(lease(root, dialogue));

        assertTrue(registry.acquire(lease(PlayerActorOwnerId.cutscene("playback-b"))).failure().isPresent());
    }

    private PlayerControlLease lease(PlayerActorOwnerId owner, PlayerActorOwnerId... lineage) {
        return new PlayerControlLease(PlayerControlLeaseId.create(), actorId, PlayerControlScopeId.CINEMATIC_CONTROL,
                PlayerControlMode.CINEMATIC_CONTROL, owner, List.of(lineage), 1L, 1L, PlayerControlStatus.ACTIVE, 1L);
    }
}
