package io.github.kxyr0.echotriggervisibilityadapter.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceLookupEvent;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceLookupEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;

public final class VisibilityInspectCommand {
    private VisibilityInspectCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_visibility")
                .then(Commands.literal("inspect").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("watch_id", StringArgumentType.word())
                                .executes(context -> inspect(context.getSource(),
                                        StringArgumentType.getString(context, "watch_id"))))));
    }

    private static int inspect(CommandSourceStack source, String raw) throws CommandSyntaxException {
        VisibilityWatchId watchId = new VisibilityWatchId(raw);
        VisibilityServiceLookupEvent visibility = new VisibilityServiceLookupEvent(source.getServer());
        TriggerServiceLookupEvent trigger = new TriggerServiceLookupEvent(source.getServer());
        MinecraftForge.EVENT_BUS.post(visibility);
        MinecraftForge.EVENT_BUS.post(trigger);
        if (visibility.bridge().isEmpty() || trigger.bridge().isEmpty()) {
            source.sendFailure(Component.literal("Visibility/Trigger provider unavailable."));
            return 0;
        }
        var player = source.getPlayerOrException();
        var view = visibility.bridge().orElseThrow();
        String state = view.query().find(player, watchId).map(value ->
                value.status() + "/" + value.state() + " gen=" + value.generation().value())
                .orElse("NOT_ARMED");
        long leases = view.concealment().leases(player).stream()
                .filter(value -> value.watchId().equals(watchId)).count();
        int subscriptions = subscriptions(trigger.bridge().orElseThrow(), raw);
        int diagnostics = view.diagnostics().recent(java.util.Optional.of(watchId), 20).size();
        source.sendSuccess(() -> Component.literal("Visibility " + raw + " state=" + state
                + " subscriptions=" + subscriptions + " leases=" + leases
                + " diagnostics=" + diagnostics), false);
        return 1;
    }

    private static int subscriptions(
            io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceBridge trigger,
            String watchId) {
        int result = 0;
        for (String kind : java.util.List.of("LOST_SIGHT", "GAINED_SIGHT", "TARGET_IS_OUT_OF_VIEW")) {
            result += trigger.subscriptions().storySubscriptions(TriggerEventSelector.of(
                    "echo_visibility:events", kind, watchId)).size();
        }
        return result;
    }
}
