package io.github.kxyr0.echoiteminspection.client.render;

import io.github.kxyr0.echoiteminspection.client.input.WorldInspectionInput;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionRuntime;
import io.github.kxyr0.echoiteminspection.world.WorldInspectionPromptLayout;
import io.github.kxyr0.echoiteminspection.forge.InspectionClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiEvent;

public final class WorldInspectionPromptRenderer {
    private WorldInspectionPromptRenderer() { }

    public static Component label() {
        return Component.literal("[").append(WorldInspectionInput.INSPECT.getTranslatedKeyMessage())
                .append("] ").append(Component.translatable("echo_item_inspection.world.inspect"));
    }

    public static void render(RenderGuiEvent.Post event) {
        WorldInspectionRuntime.refresh();
        var prompt = WorldInspectionRuntime.prompt();
        double alpha = prompt.alpha(Util.getMillis());
        if (prompt.displayTarget() == null || alpha < 0.02) return;
        var minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        var text = label();
        var layout = WorldInspectionPromptLayout.resolve(window.getWidth(), window.getHeight(), minecraft.font.width(text),
                InspectionClientConfig.PROMPT_SCALE.get(), InspectionClientConfig.PROMPT_MIN_WIDTH.get(),
                InspectionClientConfig.PROMPT_HEIGHT.get());
        var graphics = event.getGuiGraphics();
        var pose = graphics.pose();
        pose.pushPose();
        try {
            float physical = (float) (1 / window.getGuiScale());
            pose.scale(physical, physical, 1);
            pose.translate(layout.x(), layout.y() + 8 * layout.scale() * (1 - alpha), 0);
            int opacity = (int) Math.round(alpha * 255);
            graphics.fill(0, 0, (int) layout.width(), (int) layout.height(),
                    ((int) (opacity * 0.85) << 24) | 0x181020);
            graphics.fill(0, 0, (int) Math.max(1, Math.round(2 * layout.scale())),
                    (int) layout.height(), (opacity << 24) | 0xF09A3E);
            pose.translate(12 * layout.scale(), (layout.height() - 18 * layout.scale()) / 2, 0);
            pose.scale((float) (2 * layout.scale()), (float) (2 * layout.scale()), 1);
            graphics.drawString(minecraft.font, text, 0, 0, (opacity << 24) | 0xF2EDF5, false);
            graphics.flush();
        } finally { pose.popPose(); }
    }
}
