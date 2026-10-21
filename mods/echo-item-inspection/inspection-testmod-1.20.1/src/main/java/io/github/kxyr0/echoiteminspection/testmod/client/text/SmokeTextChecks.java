package io.github.kxyr0.echoiteminspection.testmod.client.text;

import io.github.kxyr0.echoiteminspection.client.api.ItemInspectionClient;
import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.client.render.InspectionBackground;
import io.github.kxyr0.echoiteminspection.forge.InspectionRuntime;
import io.github.kxyr0.echoiteminspection.testmod.client.SmokeEvidence;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Text and shader-background integration checks run only inside the separate dev client. */
public final class SmokeTextChecks {
    private final SmokeEvidence evidence;
    private final SmokeTextFixture fixture = new SmokeTextFixture();
    private final int[] scales = {1, 2, 3, 4, 0};
    private InventoryScreen parent;
    private int phase;
    private int scaleIndex;
    private int expectedLines;
    private int originalGuiScale;
    private int originalWidth;
    private int originalHeight;
    private int sampleX;
    private int sampleY;
    private int baseline;

    public SmokeTextChecks(SmokeEvidence evidence) { this.evidence = evidence; }

    public boolean tick(Minecraft minecraft) throws IOException {
        switch (phase) {
            case 0 -> {
                evidence.check("textFixtureItemInitiallyUnbound", InspectionRuntime.catalog().resolve("minecraft:clock").isEmpty());
                fixture.create(minecraft.gameDirectory.toPath());
                originalGuiScale = minecraft.options.guiScale().get();
                originalWidth = minecraft.getWindow().getWidth();
                originalHeight = minecraft.getWindow().getHeight();
                parent = new InventoryScreen(minecraft.player);
                minecraft.setScreen(parent);
                phase = 1;
            }
            case 1 -> {
                if (InspectionRuntime.catalog().resolve("minecraft:clock").isEmpty()) return false;
                evidence.capture("text-parent-baseline");
                sampleX = (int) ((parent.getGuiLeft() + 4) * minecraft.getWindow().getGuiScale());
                sampleY = (int) ((parent.getGuiTop() + 4) * minecraft.getWindow().getGuiScale());
                baseline = sample(minecraft);
                open();
                phase = 2;
            }
            case 2 -> {
                var viewer = viewer();
                evidence.check("authoredTitleVisible", viewer.textPanel().title().equals(SmokeTextFixture.TITLE));
                evidence.check("authoredUnicodeParagraphsPreserved", viewer.textPanel().description().equals(SmokeTextFixture.SHORT));
                evidence.check("shortDescriptionWrapped", viewer.textPanel().lineCount() > 3);
                int background = sample(minecraft);
                evidence.record("parentPixelBeforeBackground", baseline);
                evidence.record("shaderBackgroundPixel", background);
                evidence.check("blackspaceBackgroundShaderLoaded", InspectionBackground.shaderReady());
                evidence.check("darkBackgroundReplacesInventory", baseline > 80 && background > 20 && background < 75);
                evidence.capture("text-short-blackspace");
                viewer.onClose();
                fixture.longDescription();
                open();
                phase = 3;
            }
            case 3 -> {
                SmokeTextControls.check(viewer(), evidence);
                expectedLines = viewer().textPanel().lineCount();
                evidence.capture("text-long-start");
                phase = 4;
            }
            case 4 -> {
                if (scaleIndex == scales.length) {
                    GLFW.glfwSetWindowSize(minecraft.getWindow().getWindow(), 854, 480);
                    phase = 6;
                } else {
                    minecraft.options.guiScale().set(scales[scaleIndex]);
                    minecraft.resizeDisplay();
                    phase = 5;
                }
            }
            case 5 -> {
                int requested = scales[scaleIndex++];
                evidence.check("textWrapStableAtGuiScale" + requested, viewer().textPanel().lineCount() == expectedLines);
                evidence.capture("text-gui-scale-" + requested);
                phase = 4;
            }
            case 6 -> {
                evidence.check("narrowDescriptionBelowModel", !viewer().textPanel().layout().sideColumn());
                evidence.check("narrowDescriptionStillScrollable", viewer().textPanel().scroll().canScroll());
                evidence.capture("text-narrow");
                viewer().keyPressed(GLFW.GLFW_KEY_END, 0, 0);
                phase = 7;
            }
            case 7 -> {
                evidence.check("narrowDescriptionEndReachable", viewer().textPanel().scroll().position()
                        == viewer().textPanel().scroll().maximum());
                evidence.capture("text-narrow-end");
                viewer().onClose();
                evidence.check("textEscRestoresSameInventory", minecraft.screen == parent);
                parent.onClose();
                minecraft.options.guiScale().set(originalGuiScale);
                GLFW.glfwSetWindowSize(minecraft.getWindow().getWindow(), originalWidth, originalHeight);
                minecraft.resizeDisplay();
                fixture.cleanup();
                evidence.check("textFixtureCleaned", InspectionRuntime.catalog().resolve("minecraft:clock").isEmpty());
                return true;
            }
            default -> throw new AssertionError("Unknown text smoke phase");
        }
        return false;
    }

    public void cleanup() throws IOException { fixture.cleanup(); }

    private void open() {
        evidence.check("textViewerOpened", ItemInspectionClient.openInspection(new ItemStack(Items.CLOCK)));
    }

    private ItemInspectionScreen viewer() { return (ItemInspectionScreen) Minecraft.getInstance().screen; }

    private int sample(Minecraft minecraft) {
        try (var image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            int color = image.getPixelRGBA(sampleX, sampleY);
            return ((color & 255) + ((color >>> 8) & 255) + ((color >>> 16) & 255)) / 3;
        }
    }
}
