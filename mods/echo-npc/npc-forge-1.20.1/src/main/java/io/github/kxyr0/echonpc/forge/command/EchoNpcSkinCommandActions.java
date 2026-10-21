package io.github.kxyr0.echonpc.forge.command;

import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeRequest;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcHost;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

final class EchoNpcSkinCommandActions {
    private final Supplier<Optional<ForgeNpcHost>> host;

    EchoNpcSkinCommandActions(Supplier<Optional<ForgeNpcHost>> host) {
        this.host = java.util.Objects.requireNonNull(host, "host");
    }

    int get(CommandContext<CommandSourceStack> context) {
        ActorId npcId = npcId(context);
        host.get().ifPresentOrElse(value -> send(context, value.query(npcId).actor()
                .flatMap(actor -> actor.skinId().map(skin -> "npc=" + npcId + " skin=" + skin))
                .orElse("NPC или его скин не найден.")),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int set(CommandContext<CommandSourceStack> context) {
        ActorId npcId = npcId(context);
        NpcSkinId skinId = new NpcSkinId(ResourceLocationArgument.getId(context, "skinId").toString());
        host.get().ifPresentOrElse(value -> send(context,
                EchoNpcCommandFormatting.mutation(value.changeSkin(new NpcSkinChangeRequest(npcId, skinId)))),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private ActorId npcId(CommandContext<CommandSourceStack> context) {
        return new ActorId(ResourceLocationArgument.getId(context, "npcId").toString());
    }

    private void send(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
