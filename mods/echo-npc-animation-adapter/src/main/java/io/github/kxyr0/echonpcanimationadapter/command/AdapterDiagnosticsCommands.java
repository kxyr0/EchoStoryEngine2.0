package io.github.kxyr0.echonpcanimationadapter.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echonpcanimationadapter.lifecycle.NpcAnimationLifecycleAdapter;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingCatalogState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Runtime diagnostics for the NPC-animation lifecycle bridge.
 */
final class AdapterDiagnosticsCommands {
    private final NpcAnimationBindingCatalogState bindings;
    private final NpcAnimationLifecycleAdapter lifecycle;

    AdapterDiagnosticsCommands(
            NpcAnimationBindingCatalogState bindings,
            NpcAnimationLifecycleAdapter lifecycle
    ) {
        this.bindings = java.util.Objects.requireNonNull(bindings, "bindings");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
    }

    void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_npc_animation")
                .then(Commands.literal("health").executes(context -> health(context.getSource())))
                .then(Commands.literal("bindings").executes(context -> bindings(context.getSource()))));
    }

    private int health(CommandSourceStack source) {
        var metrics = lifecycle.lastMetrics();
        source.sendSuccess(() -> Component.literal("Echo NPC Animation: bindings="
                + bindings.current().bindings().size()
                + ", diagnostics=" + bindings.diagnostics().size()
                + ", scanned=" + metrics.scanned()
                + ", created=" + metrics.created()
                + ", existing=" + metrics.existing()
                + ", rebind_pending=" + metrics.rebindPending()
                + ", unbound=" + metrics.unbound()
                + ", conflicts=" + metrics.conflicts()
                + ", retired=" + metrics.retired()
                + ", truncated=" + metrics.truncated()
                + ", status=" + metrics.status()), false);
        return bindings.current().bindings().size();
    }

    private int bindings(CommandSourceStack source) {
        var entries = bindings.current().bindings().stream()
                .map(binding -> binding.id() + " " + binding.specificity()
                        + binding.actorId().map(id -> " actor=" + id).orElse("")
                        + binding.definitionId().map(id -> " npc=" + id).orElse("")
                        + " profile=" + binding.profileId())
                .sorted()
                .toList();
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Echo NPC Animation bindings: none"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Echo NPC Animation bindings: "
                + String.join(", ", entries)), false);
        return entries.size();
    }
}
