package io.github.kxyr0.echodialogue.forge.client.ui.image.paint;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDrawCommand;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Boundary around Minecraft GUI texture blitting.
 */
public final class UiTextureBlitter {
    private UiTextureBlitter() {
    }

    public static void render(GuiGraphics graphics, List<UiImageDrawCommand> commands) {
        for (UiImageDrawCommand command : commands) {
            graphics.blit(command.texture(), command.x(), command.y(),
                    command.targetWidth(), command.targetHeight(),
                    command.sourceX(), command.sourceY(),
                    command.sourceWidth(), command.sourceHeight(),
                    command.textureWidth(), command.textureHeight());
        }
    }
}
