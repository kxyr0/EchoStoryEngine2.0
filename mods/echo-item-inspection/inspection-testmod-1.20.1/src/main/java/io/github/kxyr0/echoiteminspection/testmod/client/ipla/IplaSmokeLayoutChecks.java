package io.github.kxyr0.echoiteminspection.testmod.client.ipla;

import com.akicater.blocks.LayingItemEntity;
import io.github.kxyr0.echoiteminspection.client.integration.ipla.IplaInspectionTargetResolver;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionRuntime;
import io.github.kxyr0.echoiteminspection.client.render.WorldInspectionPromptRenderer;
import io.github.kxyr0.echoiteminspection.forge.InspectionClientConfig;
import io.github.kxyr0.echoiteminspection.world.WorldInspectionPromptLayout;
import net.minecraftforge.client.ClientCommandHandler;
import java.lang.management.ManagementFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.lwjgl.glfw.GLFW;

final class IplaSmokeLayoutChecks {
    private final IplaSmokeEvidence evidence;
    private int phase;
    private int index;
    private final int[] scales = {1, 2, 3, 4, 0};
    private final int[][] sizes = {{1280, 720}, {2560, 1440}, {3440, 1440}, {1920, 1080}};

    IplaSmokeLayoutChecks(IplaSmokeEvidence evidence) { this.evidence = evidence; }

    boolean tick(Minecraft minecraft) {
        if (phase == 0 && index < scales.length) {
            if (index == 0) command("reset");
            minecraft.options.guiScale().set(scales[index]);
            minecraft.resizeDisplay();
            phase = 1;
            return false;
        }
        if (phase == 1) {
            checkCorner(minecraft, "guiScale" + scales[index]);
            evidence.capture("prompt-scale-" + scales[index++]);
            phase = index == scales.length ? 2 : 0;
            if (phase == 2) index = 0;
            return false;
        }
        if (phase == 2) {
            GLFW.glfwSetWindowSize(minecraft.getWindow().getWindow(), sizes[index][0], sizes[index][1]);
            phase = 3;
            return false;
        }
        if (phase == 4) {
            checkCustomSize(minecraft);
            command("reset");
            phase = 5;
            return false;
        }
        if (phase == 5) {
            checkCorner(minecraft, "resetPrompt");
            evidence.check("promptResetDefaults", InspectionClientConfig.PROMPT_SCALE.get() == 1
                    && InspectionClientConfig.PROMPT_MIN_WIDTH.get() == 0 && InspectionClientConfig.PROMPT_HEIGHT.get() == 36);
            return true;
        }
        checkCorner(minecraft, "resolution" + sizes[index][0]);
        evidence.capture("prompt-resolution-" + sizes[index++][0]);
        if (index == sizes.length) {
            command("scale 1.5");
            command("width 360");
            command("height 60");
            phase = 4;
            return false;
        }
        phase = 2;
        return false;
    }

    private void command(String arguments) {
        try {
            int result = ClientCommandHandler.getDispatcher().execute("echo_item_inspection prompt " + arguments,
                    ClientCommandHandler.getSource());
            evidence.check("promptCommand:" + arguments, result == 1);
        } catch (Exception exception) { throw new AssertionError("Prompt command failed: " + arguments, exception); }
    }

    private void checkCustomSize(Minecraft minecraft) {
        evidence.check("customPromptSettingsApplied", InspectionClientConfig.PROMPT_SCALE.get() == 1.5
                && InspectionClientConfig.PROMPT_MIN_WIDTH.get() == 360 && InspectionClientConfig.PROMPT_HEIGHT.get() == 60);
        var layout = WorldInspectionPromptLayout.resolve(minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight(), minecraft.font.width(WorldInspectionPromptRenderer.label()), 1.5, 360, 60);
        try (var image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            int accent = image.getPixelRGBA((int) layout.x(), (int) layout.y() + 5);
            evidence.check("customPromptPhysicalAccent", (accent & 255) > 180
                    && ((accent >>> 8) & 255) > ((accent >>> 16) & 255) + 30);
        }
        evidence.capture("prompt-custom-size");
        try {
            String config = java.nio.file.Files.readString(minecraft.gameDirectory.toPath()
                    .resolve("config/echo_item_inspection-client.toml"));
            evidence.check("customPromptSettingsPersisted", config.contains("promptScale = 1.5")
                    && config.contains("promptMinWidth = 360") && config.contains("promptHeight = 60"));
            ClientCommandHandler.getDispatcher().execute("echo_item_inspection prompt scale 99", ClientCommandHandler.getSource());
            throw new AssertionError("Out-of-range scale accepted");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
            evidence.check("invalidPromptScaleDoesNotMutate", InspectionClientConfig.PROMPT_SCALE.get() == 1.5);
        } catch (java.io.IOException exception) { throw new AssertionError("Prompt config was not saved", exception); }
    }

    private void checkCorner(Minecraft minecraft, String name) {
        evidence.check(name + "TargetActive", WorldInspectionRuntime.prompt().activeTarget() != null);
        try (var image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            double scale = Math.min(image.getWidth() / 1920.0, image.getHeight() / 1080.0);
            int x = (int) Math.round(24 * scale);
            int y = image.getHeight() - (int) Math.round(68 * scale);
            int accent = image.getPixelRGBA(x, y + Math.max(2, (int) (5 * scale)));
            int red = accent & 255;
            int green = (accent >>> 8) & 255;
            int blue = (accent >>> 16) & 255;
            evidence.check(name + "PhysicalPromptAccent", red > 180 && red > green + 30 && green > blue + 30);
            evidence.record(name + "PhysicalCorner", x + "," + y);
        }
    }

    void performance(Minecraft minecraft) {
        var resolver = new IplaInspectionTargetResolver();
        for (int i = 0; i < 200; i++) resolver.resolve();
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().getId();
        long before = bean.getThreadAllocatedBytes(thread);
        long start = System.nanoTime();
        int maximumCandidates = 0;
        for (int i = 0; i < 1000; i++) {
            evidenceTarget(resolver);
            maximumCandidates = Math.max(maximumCandidates, resolver.candidatesExamined());
        }
        evidence.record("nanosecondsPerSpatialQuery", (System.nanoTime() - start) / 1000);
        evidence.record("bytesAllocatedPerSpatialQuery", (bean.getThreadAllocatedBytes(thread) - before) / 1000);
        evidence.record("maximumCandidatesPerQueryWith102PlacedItems", maximumCandidates);
        evidence.record("spatialQueries", resolver.queries());
        evidence.check("noGlobalPlacedItemScan", maximumCandidates <= 24);
        evidence.check("unchangedHitUsesCachedSlot", maximumCandidates == 0);
        evidence.check("placedObjectStillPresent", minecraft.level.getBlockEntity(IplaSmokeFixture.ITEM) instanceof LayingItemEntity);
        movingCamera(minecraft, resolver, bean, thread);
    }

    private void movingCamera(Minecraft minecraft, IplaInspectionTargetResolver resolver,
            com.sun.management.ThreadMXBean bean, long thread) {
        float yaw = minecraft.player.getYRot();
        int maximum = 0;
        long bytes = bean.getThreadAllocatedBytes(thread);
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            minecraft.player.setYRot(yaw + (float) Math.sin(i) * 0.05F);
            minecraft.gameRenderer.getMainCamera().setup(minecraft.level, minecraft.player, false, false, 1);
            evidenceTarget(resolver);
            maximum = Math.max(maximum, resolver.candidatesExamined());
        }
        evidence.record("movingCameraNanosecondsPerQuery", (System.nanoTime() - start) / 1000);
        evidence.record("movingCameraBytesPerQuery", (bean.getThreadAllocatedBytes(thread) - bytes) / 1000);
        evidence.record("movingCameraMaximumCandidatesWith102Items", maximum);
        evidence.check("movingCameraOnlyExaminesHitBlock", maximum == 24);
        minecraft.player.setYRot(yaw);
        minecraft.gameRenderer.getMainCamera().setup(minecraft.level, minecraft.player, false, false, 1);
    }

    private void evidenceTarget(IplaInspectionTargetResolver resolver) {
        if (resolver.resolve().isEmpty()) throw new AssertionError("Performance target missing");
    }
}
