package io.github.kxyr0.echonpcinteraction.forge.command;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionExecutionId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionOwnerId;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.target.NpcInteractionTarget;
import net.minecraft.commands.CommandSourceStack;

final class CommandInteractionRequests {
    private CommandInteractionRequests() {
    }

    static NpcInteractionRequest interact(CommandSourceStack source, ActorId actorId, BlockPosition position) {
        String dimension = source.getLevel().dimension().location().toString();
        long tick = source.getLevel().getGameTime();
        String seed = source.getTextName() + "|" + actorId.value() + "|" + position + "|" + tick;
        String hash = Integer.toUnsignedString(seed.hashCode(), 36);
        return new NpcInteractionRequest(
                new NpcInteractionCommandId("echo_npc_interaction:cmd_" + hash),
                new NpcInteractionExecutionId("echo_npc_interaction:command_" + hash),
                new NpcInteractionOwnerId("echo_npc_interaction:command"),
                actorId,
                new NpcInteractionTarget(dimension, position),
                "AUTO",
                "MAIN_HAND",
                "AUTO",
                tick);
    }
}
