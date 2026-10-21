package io.github.kxyr0.echonpcinteraction.core.command;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionExecutionId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionOwnerId;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DeterministicInteractionIds {
    private static final String MOD_ID = "echo_npc_interaction";

    private DeterministicInteractionIds() {
    }

    public static NpcInteractionCommandId command(String ownerExecution, ActorId npcId, BlockPosition position) {
        return new NpcInteractionCommandId(MOD_ID + ":cmd_" + hash(ownerExecution, npcId, position));
    }

    public static NpcInteractionExecutionId execution(String ownerExecution) {
        return new NpcInteractionExecutionId(MOD_ID + ":exec_" + hash(ownerExecution));
    }

    public static NpcInteractionOwnerId owner(String ownerExecution) {
        return new NpcInteractionOwnerId(MOD_ID + ":owner_" + hash(ownerExecution));
    }

    private static String hash(String ownerExecution, ActorId npcId, BlockPosition position) {
        return hash(ownerExecution + "|" + npcId.value() + "|" + position.x() + "|" + position.y()
                + "|" + position.z());
    }

    private static String hash(String value) {
        return UUID.nameUUIDFromBytes(String.valueOf(value).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
    }
}
