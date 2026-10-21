package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ExtensionCatalog;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostSnapshot;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Forge adapter for the read-only Brigadier command tree `/echo_story`.
 */
public final class ForgeDiagnosticsCommand {
    private ForgeDiagnosticsCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandRuntimeView view) {
        EchoStoryCommandTree.CommandSurface<CommandSourceStack> surface =
                new EchoStoryCommandTree.CommandSurface<>() {
                    @Override
                    public boolean hasPermission(CommandSourceStack source) {
                        return source.hasPermission(Commands.LEVEL_GAMEMASTERS);
                    }

                    @Override
                    public void send(CommandSourceStack source, String line) {
                        source.sendSuccess(() -> Component.literal(line), false);
                    }
                };
        new EchoStoryCommandTree<>(view.buffer(), view.hostSnapshot(), view.catalogState(), view.extensionCatalog(),
                surface)
                .register(dispatcher);
    }

    public record CommandRuntimeView(
            InMemoryDiagnosticBuffer buffer,
            Supplier<Optional<HostSnapshot>> hostSnapshot,
            StoryCatalogState catalogState,
            Supplier<ExtensionCatalog> extensionCatalog) {
    }
}
