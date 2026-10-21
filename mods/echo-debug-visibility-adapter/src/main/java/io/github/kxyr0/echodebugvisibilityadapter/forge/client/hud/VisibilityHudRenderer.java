package io.github.kxyr0.echodebugvisibilityadapter.forge.client.hud;

import io.github.kxyr0.echodebugvisibilityadapter.forge.client.VisibilityDebugAccess;
import io.github.kxyr0.echodebugvisibilityadapter.forge.client.VisibilityDebugSettings;
import io.github.kxyr0.echovisibility.api.debug.VisibilityDebugSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class VisibilityHudRenderer {
    private final VisibilityDebugSettings settings;
    private final VisibilityDebugAccess access;

    public VisibilityHudRenderer(VisibilityDebugSettings settings, VisibilityDebugAccess access) {
        this.settings = settings;
        this.access = access;
    }

    @SubscribeEvent
    public void render(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!settings.active() || minecraft.player == null) {
            return;
        }
        access.view().ifPresent(view -> draw(event, minecraft, view.camera().map(camera ->
                "camera=" + camera.source() + " fov=" + format(camera.dynamicFovDegrees())
                        + " aspect=" + format(camera.aspectRatio())).orElse("camera=unavailable"),
                access.watches(view, settings)));
    }

    private static void draw(
            RenderGuiOverlayEvent.Post event,
            Minecraft minecraft,
            String camera,
            List<VisibilityDebugSnapshot> snapshots) {
        List<String> lines = new ArrayList<>();
        lines.add("Echo Visibility | " + camera);
        for (VisibilityDebugSnapshot snapshot : snapshots) {
            lines.add(snapshot.definition().id().value() + " gen=" + snapshot.generation().value()
                    + " state=" + snapshot.state() + " frustum=" + snapshot.frustum());
            lines.add(" samples=" + snapshot.samples().size() + " hidden=" + snapshot.hiddenFrames()
                    + " visible=" + snapshot.visibleFrames() + " conceal=" + snapshot.concealmentStatus());
            lines.add(" report=" + snapshot.reportSequence() + " ack=" + snapshot.serverAcknowledgement()
                    + " reason=" + snapshot.transition().map(value -> value.kind().name()).orElse("NONE"));
        }
        int width = Math.min(minecraft.getWindow().getGuiScaledWidth() - 12, 620);
        int height = Math.min(minecraft.getWindow().getGuiScaledHeight() - 12, 8 + lines.size() * 10);
        var graphics = event.getGuiGraphics();
        graphics.enableScissor(5, 5, Math.max(6, width), Math.max(6, height));
        graphics.fill(5, 5, Math.max(6, width), Math.max(6, height), 0xB010141C);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(minecraft.font, Component.literal(lines.get(index)), 9, 9 + index * 10,
                    0xF2F5F7, false);
        }
        graphics.disableScissor();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
