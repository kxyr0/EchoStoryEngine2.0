package io.github.kxyr0.echoiteminspection.forge.resource;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLPaths;

/** Exports actual registry identities for the offline Doctor; contains no stack or player data. */
public final class InspectionItemCatalog {
    private InspectionItemCatalog() { }

    public static void export(Set<String> itemIds) {
        var destination = FMLPaths.CONFIGDIR.get().resolve("echo_item_inspection/item-catalog.txt");
        String content = String.join("\n", itemIds.stream().sorted().toList()) + "\n";
        try {
            if (Files.isRegularFile(destination) && Files.readString(destination).equals(content)) return;
            Files.createDirectories(destination.getParent());
            var temporary = destination.resolveSibling(destination.getFileName() + ".writing");
            Files.writeString(temporary, content);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Cannot export Item Inspection registry catalog: {}", exception.getMessage());
        }
    }
}
