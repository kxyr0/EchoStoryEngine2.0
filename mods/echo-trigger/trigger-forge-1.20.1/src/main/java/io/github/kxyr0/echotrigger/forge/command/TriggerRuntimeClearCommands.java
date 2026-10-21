package io.github.kxyr0.echotrigger.forge.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

final class TriggerRuntimeClearCommands {
    private TriggerRuntimeClearCommands() { }
    static int clearRuntime(CommandContext<CommandSourceStack> context, ForgeTriggerServices services) {
        int removed = services.clearRuntime();
        send(context, "Echo Trigger runtime cleared: scope=all removed=" + removed);
        return 1;
    }

    static int clearPlayerRuntime(
            CommandContext<CommandSourceStack> context,
            ForgeTriggerServices services) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TriggerOwnerId owner = ForgeIds.owner(player);
        int removed = services.clearRuntime(owner);
        send(context, "Echo Trigger runtime cleared: scope=player owner=" + owner.value()
                + " removed=" + removed);
        return 1;
    }

    static int clearTriggerRuntime(
            CommandContext<CommandSourceStack> context,
            ForgeTriggerServices services) {
        String id = StringArgumentType.getString(context, "trigger_id");
        try {
            TriggerId triggerId = TriggerId.parse(id);
            int removed = services.clearRuntime(triggerId);
            send(context, "Echo Trigger runtime cleared: scope=trigger trigger=" + triggerId.value()
                    + " removed=" + removed);
            return 1;
        } catch (IllegalArgumentException exception) {
            send(context, "Echo Trigger runtime clear FAILED: invalid trigger_id=" + id);
            return 0;
        }
    }

    static int clearPlayerTriggerRuntime(
            CommandContext<CommandSourceStack> context,
            ForgeTriggerServices services) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "trigger_id");
        try {
            TriggerId triggerId = TriggerId.parse(id);
            ServerPlayer player = context.getSource().getPlayerOrException();
            TriggerOwnerId owner = ForgeIds.owner(player);
            int removed = services.clearRuntime(triggerId, owner);
            send(context, "Echo Trigger runtime cleared: scope=player+trigger owner=" + owner.value()
                    + " trigger=" + triggerId.value() + " removed=" + removed);
            return 1;
        } catch (IllegalArgumentException exception) {
            send(context, "Echo Trigger runtime clear FAILED: invalid trigger_id=" + id);
            return 0;
        }
    }

    private static void send(CommandContext<CommandSourceStack> context, String text) {
        context.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
