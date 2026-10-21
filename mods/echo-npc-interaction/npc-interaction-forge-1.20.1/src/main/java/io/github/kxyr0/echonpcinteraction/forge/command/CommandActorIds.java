package io.github.kxyr0.echonpcinteraction.forge.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import net.minecraft.commands.CommandSourceStack;

final class CommandActorIds {
    private static final String JDH_DEFAULT_NAMESPACE = "jdh";

    private CommandActorIds() {
    }

    static ActorId actor(CommandContext<CommandSourceStack> context) {
        String raw = StringArgumentType.getString(context, "npc_id").trim();
        return new ActorId(raw.contains(":") ? raw : JDH_DEFAULT_NAMESPACE + ":" + raw);
    }
}
