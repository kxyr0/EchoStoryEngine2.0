package io.github.kxyr0.echonpcnavigationadapter.script;

import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DeterministicNavigationIds {
    private static final String MOD_ID = "echo_navigation";

    private DeterministicNavigationIds() {
    }

    public static NavigationExecutionId execution(String ownerExecutionId) {
        return new NavigationExecutionId(MOD_ID + ":exec_" + hash(ownerExecutionId));
    }

    public static NavigationOwnerId owner(String ownerExecutionId) {
        return new NavigationOwnerId(MOD_ID + ":owner_" + hash(ownerExecutionId));
    }

    public static NavigationCommandId command(String ownerExecutionId, ActorId actorId, int x, int y, int z) {
        return new NavigationCommandId(MOD_ID + ":cmd_" + hash(ownerExecutionId, actorId, x, y, z));
    }

    public static NavigationCommandId command(String ownerExecutionId, NpcNavigationPayloads.MoveCommand command) {
        return new NavigationCommandId(MOD_ID + ":cmd_" + hash(ownerExecutionId, command));
    }

    public static NavigationMoveId move(String ownerExecutionId, ActorId actorId, int x, int y, int z) {
        return new NavigationMoveId(MOD_ID + ":move_" + hash(ownerExecutionId, actorId, x, y, z));
    }

    public static NavigationMoveId move(String ownerExecutionId, NpcNavigationPayloads.MoveCommand command) {
        return new NavigationMoveId(MOD_ID + ":move_" + hash(ownerExecutionId, command));
    }

    private static String hash(String ownerExecutionId, NpcNavigationPayloads.MoveCommand command) {
        var destination = command.destination();
        return hash(ownerExecutionId + "|" + command.actorId().value() + "|" + destination.dimensionId()
                + "|" + destination.blockX() + "|" + destination.blockY() + "|" + destination.blockZ()
                + "|" + command.gait() + "|" + command.speedBlocksPerSecond()
                + "|" + command.synchronizeAnimationSpeed());
    }

    private static String hash(String ownerExecutionId, ActorId actorId, int x, int y, int z) {
        return hash(ownerExecutionId + "|" + actorId.value() + "|" + x + "|" + y + "|" + z);
    }

    private static String hash(String value) {
        return UUID.nameUUIDFromBytes(String.valueOf(value).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
    }
}
