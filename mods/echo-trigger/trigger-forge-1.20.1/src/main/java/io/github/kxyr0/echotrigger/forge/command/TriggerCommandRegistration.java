package io.github.kxyr0.echotrigger.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaDiagnosticView;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.forge.authoring.ForgeTriggerAuthoringService;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class TriggerCommandRegistration {
    private TriggerCommandRegistration() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            ForgeTriggerServices services,
            ForgeTriggerAuthoringService authoringService) {
        dispatcher.register(Commands.literal("echo_trigger")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(context -> list(context, services)))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context, authoringService)))
                .then(Commands.literal("subscriptions")
                        .then(Commands.literal("sleep")
                                .executes(context -> sleepSubscriptions(context, services)))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("subscription_id", StringArgumentType.greedyString())
                                        .executes(context -> inspect(context, services))))
                        .then(Commands.literal("conflicts")
                                .executes(context -> conflicts(context, services))))
                .then(Commands.literal("runtime")
                        .then(Commands.literal("clear")
                                .executes(context -> TriggerRuntimeClearCommands.clearRuntime(context, services))
                                .then(Commands.literal("player")
                                        .executes(context -> TriggerRuntimeClearCommands.clearPlayerRuntime(
                                                        context, services))
                                        .then(Commands.literal("trigger")
                                                .then(Commands.argument("trigger_id",
                                                        StringArgumentType.greedyString())
                                                        .executes(context -> TriggerRuntimeClearCommands
                                                                .clearPlayerTriggerRuntime(
                                                                context, services)))))
                                .then(Commands.literal("trigger")
                                        .then(Commands.argument("trigger_id", StringArgumentType.greedyString())
                                                .executes(context -> TriggerRuntimeClearCommands.clearTriggerRuntime(
                                                        context, services))))))
                .then(Commands.literal("diagnostics")
                        .executes(context -> diagnostics(context, services))));
    }

    private static int sleepSubscriptions(CommandContext<CommandSourceStack> context,
            ForgeTriggerServices services) {
        String text = services.storySubscriptions().isEmpty() ? "No sleep Story subscriptions."
                : services.storySubscriptions().stream()
                        .map(TriggerCommandRegistration::subscriptionText)
                        .collect(Collectors.joining(" | "));
        send(context, text);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context, ForgeTriggerServices services) {
        List<String> ids = services.definitions().stream()
                .map(definition -> definition.id().value())
                .sorted()
                .toList();
        send(context, ids.isEmpty() ? "Trigger catalog is empty." : "Trigger IDs: " + String.join(", ", ids));
        return Math.max(1, ids.size());
    }

    private static int inspect(CommandContext<CommandSourceStack> context, ForgeTriggerServices services) {
        String id = StringArgumentType.getString(context, "subscription_id");
        Optional<TriggerStorySubscription> found = services.storySubscriptions().stream()
                .filter(subscription -> subscription.subscriptionId().value().equals(id))
                .findFirst();
        send(context, found.map(TriggerCommandRegistration::subscriptionText)
                .orElse("No Trigger subscription found for subscription_id=" + id));
        return found.isPresent() ? 1 : 0;
    }

    private static int conflicts(CommandContext<CommandSourceStack> context, ForgeTriggerServices services) {
        String text = grouped(services).entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(TriggerCommandRegistration::conflictText)
                .collect(Collectors.joining(" | "));
        send(context, text.isBlank() ? "No sleep subscription conflicts." : text);
        return text.isBlank() ? 1 : 0;
    }

    private static int diagnostics(CommandContext<CommandSourceStack> context, ForgeTriggerServices services) {
        long conflicts = grouped(services).values().stream()
                .filter(subscriptions -> subscriptions.size() > 1)
                .count();
        String text = "catalogRevision=" + services.catalogRevision()
                + " sleepSubscriptions=" + services.storySubscriptions().size()
                + " sleepConflicts=" + conflicts;
        send(context, text);
        return conflicts == 0L ? 1 : 0;
    }

    private static int reload(
            CommandContext<CommandSourceStack> context,
            ForgeTriggerAuthoringService authoringService) {
        var result = authoringService.reloadCanonicalRoot();
        if (result.successful()) {
            send(context, "Echo Trigger reload OK: revision " + result.beforeRevision()
                    + " -> " + result.afterRevision()
                    + ", definitions=" + result.definitions()
                    + ", sleepSubscriptions=" + result.sleepSubscriptions());
            return 1;
        }
        send(context, "Echo Trigger reload FAILED: " + diagnosticsText(result.diagnostics()));
        return 0;
    }

    private static Map<String, List<TriggerStorySubscription>> grouped(ForgeTriggerServices services) {
        return services.storySubscriptions().stream()
                .collect(Collectors.groupingBy(subscription -> subscription.selector().exactKey(),
                        LinkedHashMap::new, Collectors.toList()));
    }

    private static String conflictText(Map.Entry<String, List<TriggerStorySubscription>> entry) {
        String ids = entry.getValue().stream()
                .map(subscription -> subscription.subscriptionId().value())
                .collect(Collectors.joining(","));
        return "selector=" + entry.getKey() + " subscriptions=" + ids;
    }

    private static String subscriptionText(TriggerStorySubscription subscription) {
        return "subscription=" + subscription.subscriptionId().value()
                + " trigger=" + subscription.triggerId().value()
                + " selector=" + subscription.selector().exactKey()
                + " target=" + subscription.targetKind() + ":" + subscription.targetId()
                + " fireOnlyOnce=" + subscription.fireOnlyOnce();
    }

    private static String diagnosticsText(List<AreaDiagnosticView> diagnostics) {
        return diagnostics.stream()
                .map(diagnostic -> diagnostic.code() + ": " + diagnostic.message())
                .collect(Collectors.joining(" | "));
    }

    private static void send(CommandContext<CommandSourceStack> context, String text) {
        context.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
