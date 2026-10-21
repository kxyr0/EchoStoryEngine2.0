package io.github.kxyr0.echoanimation.forge.command.catalog;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
public final class CatalogCommandOutput {
    private CatalogCommandOutput() { }
    public static void sendList(CommandSourceStack source, String title, List<String> entries) {
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal(title + ": none"), false);
            return;
        }
        source.sendSuccess(() -> Component.literal(title + ": " + String.join(", ", entries)), false);
    }
}
