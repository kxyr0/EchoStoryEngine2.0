package io.github.kxyr0.echoiteminspection.testmod.client;

import io.github.kxyr0.echoiteminspection.catalog.InspectionCatalogSnapshot;
import io.github.kxyr0.echoiteminspection.client.api.ItemInspectionClient;
import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.forge.InspectionRuntime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exercises the actual external-file watcher; every write belongs to one new UUID fixture. */
final class SmokeReloadChecks {
    private final SmokeEvidence evidence;
    private Path fixture;
    private boolean owned;
    private int phase;
    private ItemInspectionScreen pinned;
    private InspectionCatalogSnapshot lastGood;

    SmokeReloadChecks(SmokeEvidence evidence) {
        this.evidence = evidence;
    }

    boolean tick(Minecraft minecraft) throws IOException {
        var definition = InspectionRuntime.catalog().resolve("minecraft:compass");
        switch (phase) {
            case 0 -> {
                evidence.check("reloadFixtureItemInitiallyUnbound", definition.isEmpty());
                fixture = minecraft.gameDirectory.toPath().resolve("story/item-inspection/echo_item_inspection_smoke")
                        .resolve("runtime_" + UUID.randomUUID().toString().replace("-", "") + ".inspection.es");
                Files.createDirectories(fixture.getParent());
                Files.writeString(fixture, source(2), StandardOpenOption.CREATE_NEW);
                owned = true;
                phase = 1;
            }
            case 1 -> {
                if (definition.isEmpty()) return false;
                evidence.check("externalFileWatcherLoaded", definition.orElseThrow().view().maxZoom() == 2);
                evidence.check("reloadInitialViewerOpened", ItemInspectionClient.openInspection(new ItemStack(Items.COMPASS)));
                pinned = (ItemInspectionScreen) minecraft.screen;
                Files.writeString(fixture, source(3));
                phase = 2;
            }
            case 2 -> {
                if (definition.isEmpty() || definition.orElseThrow().view().maxZoom() != 3) return false;
                evidence.check("fileWatcherValidReload", true);
                evidence.check("activeSessionPinnedOldRevision", pinned.session().definition().view().maxZoom() == 2
                        && !pinned.session().definition().revision().equals(definition.orElseThrow().revision()));
                pinned.onClose();
                evidence.check("reloadNextViewerOpened", ItemInspectionClient.openInspection(new ItemStack(Items.COMPASS)));
                pinned = (ItemInspectionScreen) minecraft.screen;
                evidence.check("nextSessionUsesReloadedView", pinned.session().definition().view().maxZoom() == 3);
                lastGood = InspectionRuntime.catalog().snapshot();
                Files.writeString(fixture, "class Broken : ItemInspectionResource { Item(\"missing:broken\");");
                phase = 3;
            }
            case 3 -> {
                if (!InspectionRuntime.status().contains("applied=false")) return false;
                evidence.check("brokenFileWatcherRetainsLkg", InspectionRuntime.catalog().snapshot() == lastGood);
                evidence.check("brokenReloadKeepsViewer", minecraft.screen == pinned);
                pinned.onClose();
                Files.delete(fixture);
                owned = false;
                phase = 4;
            }
            case 4 -> {
                if (definition.isPresent()) return false;
                evidence.check("removedDefinitionNoLongerInspectable",
                        !ItemInspectionClient.isInspectable(new ItemStack(Items.COMPASS)));
                evidence.check("reloadFixtureCleaned", !Files.exists(fixture));
                return true;
            }
            default -> throw new AssertionError("Unknown reload phase");
        }
        return false;
    }

    void cleanup() throws IOException {
        if (owned) {
            Files.deleteIfExists(fixture);
            owned = false;
            InspectionRuntime.reload();
        }
    }

    private static String source(int maximum) {
        return "class SmokeRuntimeInspection : ItemInspectionResource { Item(\"minecraft:compass\");"
                + " View() { Zoom(min:0.65, max:" + maximum + ", initial:1.0); } }";
    }
}
