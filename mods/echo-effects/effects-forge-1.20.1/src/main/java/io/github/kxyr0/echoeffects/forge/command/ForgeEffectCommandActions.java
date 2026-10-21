package io.github.kxyr0.echoeffects.forge.command;

import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echoeffects.api.identity.EffectCommandId;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import io.github.kxyr0.echoeffects.forge.ForgeEffectServices;
import io.github.kxyr0.echoeffects.forge.content.ForgeEffectContentLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class ForgeEffectCommandActions {
    private final ForgeEffectServices services;
    private final ForgeEffectContentLoader loader;

    ForgeEffectCommandActions(ForgeEffectServices services, ForgeEffectContentLoader loader) {
        this.services = services;
        this.loader = loader;
    }

    int reload(CommandContext<CommandSourceStack> context) {
        services.accept(loader.load());
        say(context, "Echo Effects definitions=" + services.definitions().size()
                + " diagnostics=" + services.diagnostics().size());
        return services.definitions().size();
    }

    int definitions(CommandContext<CommandSourceStack> context) {
        services.definitions().forEach(definition -> say(context, definition.id().value()
                + " durationTicks=" + definition.duration().ticks()
                + " tracks=" + definition.tracks().size()
                + " nameKey=" + definition.presentation().nameKey()
                + " descriptionKey=" + definition.presentation().descriptionKey()
                + " icon=" + definition.presentation().icon()));
        return services.definitions().size();
    }

    int diagnostics(CommandContext<CommandSourceStack> context) {
        services.diagnostics().forEach(diagnostic -> say(context,
                diagnostic.severity() + " " + diagnostic.code() + " " + diagnostic.message()));
        return services.diagnostics().size();
    }

    int active(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        EffectTargetRef target = EffectTargetRef.player(player.getUUID());
        services.activeEffects(target).forEach(snapshot -> say(context,
                snapshot.view().effectId().value() + " " + snapshot.view().status()
                        + " elapsed=" + snapshot.view().elapsedTicks()));
        return services.activeEffects(target).size();
    }

    int apply(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        EffectId effectId = EffectId.of(ResourceLocationArgument.getId(context, "effect").toString());
        EffectCommandId commandId = commandId(context, effectId);
        var result = services.applyToTarget(EffectTargetRef.player(player.getUUID()), effectId, commandId, 1.0D);
        say(context, result.map(snapshot -> "applied " + snapshot.view().instanceId().value())
                .orElse("effect apply failed"));
        return result.isPresent() ? 1 : 0;
    }

    int remove(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        EffectId effectId = EffectId.of(ResourceLocationArgument.getId(context, "effect").toString());
        boolean removed = services.removeFromTarget(EffectTargetRef.player(player.getUUID()), effectId, 0L);
        say(context, removed ? "removed " + effectId.value() : "effect was not active");
        return removed ? 1 : 0;
    }

    private EffectCommandId commandId(CommandContext<CommandSourceStack> context, EffectId effectId) {
        String tick = Long.toString(context.getSource().getLevel().getGameTime());
        return EffectCommandId.deterministic("command", context.getSource().getTextName(), tick, effectId);
    }

    private void say(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
