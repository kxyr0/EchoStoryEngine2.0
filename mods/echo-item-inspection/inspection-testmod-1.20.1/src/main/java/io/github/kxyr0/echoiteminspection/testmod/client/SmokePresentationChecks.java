package io.github.kxyr0.echoiteminspection.testmod.client;

import io.github.kxyr0.echoiteminspection.client.api.ItemInspectionClient;
import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.math.QuaternionRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class SmokePresentationChecks {
    private final SmokeEvidence evidence;
    private int phase;
    private int scaleIndex;
    private QuaternionRotation expectedOrientation;
    private double expectedZoom;
    private final int[] scales = {1, 2, 3, 4, 0};
    private final int originalScale = Minecraft.getInstance().options.guiScale().get();

    SmokePresentationChecks(SmokeEvidence evidence) {
        this.evidence = evidence;
    }

    void controls(ItemInspectionScreen viewer) {
        var transform = viewer.session().transform();
        var initial = transform.orientation();
        viewer.mouseDragged(0, 0, 0, 30, 20);
        evidence.check("noLmbNoRotation", initial.equals(transform.orientation()));
        viewer.mouseClicked(0, 0, 1);
        viewer.mouseDragged(0, 0, 1, 30, 20);
        evidence.check("rmbNoRotation", initial.equals(transform.orientation()));
        viewer.mouseClicked(0, 0, 0);
        viewer.mouseDragged(30, 20, 0, 30, 20);
        evidence.check("lmbDragRotates", !initial.equals(transform.orientation()));
        viewer.mouseReleased(30, 20, 0);
        var rotated = transform.orientation();
        viewer.mouseDragged(60, 40, 0, 30, 20);
        evidence.check("releaseStopsRotation", rotated.equals(transform.orientation()));
        double before = transform.zoom();
        viewer.mouseScrolled(0, 0, 1);
        evidence.check("wheelZooms", transform.zoom() > before);
        viewer.mouseScrolled(0, 0, 10000);
        evidence.check("zoomMaxClamped", transform.zoom() == transform.view().maxZoom());
        viewer.mouseScrolled(0, 0, -10000);
        evidence.check("zoomMinClamped", transform.zoom() == transform.view().minZoom());
        transform.reset();
        transform.drag(180, 90);
    }

    boolean tick(Minecraft minecraft) {
        switch (phase) {
            case 0 -> {
                evidence.check("blockOpened", ItemInspectionClient.openInspection(new ItemStack(Items.STONE)));
                phase = 1;
            }
            case 1 -> {
                evidence.capture("block");
                close(minecraft);
                var stack = SmokeClientFixture.customModel();
                var renderer = minecraft.getItemRenderer();
                evidence.check("customModelDataResolvesModel", renderer.getModel(stack,
                        minecraft.level, minecraft.player, 0) != renderer.getModel(SmokeClientFixture.token(),
                        minecraft.level, minecraft.player, 0));
                evidence.check("customModelOpened", ItemInspectionClient.openInspection(stack));
                phase = 2;
            }
            case 2 -> {
                evidence.capture("custom-model-data");
                close(minecraft);
                var stack = SmokeClientFixture.glint();
                stack.removeTagKey("Enchantments");
                evidence.check("baseSwordHasNoFoil", !stack.hasFoil());
                evidence.check("baseSwordOpened", ItemInspectionClient.openInspection(stack));
                phase = 3;
            }
            case 3 -> {
                evidence.capture("sword-base");
                close(minecraft);
                var stack = SmokeClientFixture.glint();
                evidence.check("glintStackHasFoil", stack.hasFoil());
                evidence.check("glintOpened", ItemInspectionClient.openInspection(stack));
                phase = 6;
            }
            case 6 -> {
                evidence.capture("glint");
                phase = 7;
            }
            case 7 -> {
                evidence.capture("glint-later");
                close(minecraft);
                evidence.check("guiScaleViewerOpened", ItemInspectionClient.openInspection(SmokeClientFixture.token()));
                var transform = ((ItemInspectionScreen) minecraft.screen).session().transform();
                expectedOrientation = transform.orientation();
                expectedZoom = transform.zoom();
                phase = 4;
            }
            case 4 -> {
                if (scaleIndex == scales.length) {
                    close(minecraft);
                    minecraft.options.guiScale().set(originalScale);
                    minecraft.resizeDisplay();
                    return true;
                }
                minecraft.options.guiScale().set(scales[scaleIndex]);
                minecraft.resizeDisplay();
                phase = 5;
            }
            case 5 -> {
                int requested = scales[scaleIndex++];
                var viewer = (ItemInspectionScreen) minecraft.screen;
                var transform = viewer.session().transform();
                evidence.record("guiScale" + requested, minecraft.getWindow().getGuiScale());
                evidence.record("guiScale" + requested + "Orientation", transform.orientation().toString());
                evidence.record("guiScale" + requested + "Zoom", transform.zoom());
                evidence.record("guiScale" + requested + "AuthoredScale", viewer.session().definition().view().scale());
                evidence.record("guiScale" + requested + "Viewport", viewer.width + "x" + viewer.height);
                evidence.check("guiScale" + requested + "OrientationStable", expectedOrientation.equals(transform.orientation()));
                evidence.check("guiScale" + requested + "ZoomStable", expectedZoom == transform.zoom());
                evidence.capture("gui-scale-" + requested);
                phase = 4;
            }
            default -> throw new AssertionError("Unknown presentation phase");
        }
        return false;
    }

    private static void close(Minecraft minecraft) {
        ((ItemInspectionScreen) minecraft.screen).keyPressed(256, 0, 0);
    }
}
