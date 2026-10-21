package io.github.kxyr0.echoplayeractor.core.rotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorTransform;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInterpolation;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookMode;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerLookAtResolverTest {
    private final PlayerLookAtResolver resolver = new PlayerLookAtResolver();
    private final PlayerActorId actorId = PlayerActorId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private final PlayerActorOwnerId ownerId = PlayerActorOwnerId.system("test");

    @Test
    void resolvesTargetInFront() {
        PlayerActorLookRequest request = request(new Vector3(0.0D, 1.62D, 4.0D));
        PlayerActorTransform origin = new PlayerActorTransform("minecraft:overworld",
                new Vector3(0.0D, 0.0D, 0.0D), 0.0D, 0.0D, 0.0D);

        var rotation = resolver.resolve(origin, request);

        assertEquals(0.0D, rotation.yaw(), 0.001D);
        assertEquals(0.0D, rotation.pitch(), 0.001D);
    }

    @Test
    void rejectsTooCloseTarget() {
        PlayerActorLookRequest request = request(new Vector3(0.0D, 1.62D, 0.0D));
        PlayerActorTransform origin = new PlayerActorTransform("minecraft:overworld",
                new Vector3(0.0D, 0.0D, 0.0D), 0.0D, 0.0D, 0.0D);

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(origin, request));
    }

    private PlayerActorLookRequest request(Vector3 target) {
        return new PlayerActorLookRequest(PlayerActorCommandId.create(), actorId, ownerId, target, 12,
                PlayerActorInterpolation.SMOOTH, PlayerActorLookMode.BODY_AND_HEAD);
    }
}
