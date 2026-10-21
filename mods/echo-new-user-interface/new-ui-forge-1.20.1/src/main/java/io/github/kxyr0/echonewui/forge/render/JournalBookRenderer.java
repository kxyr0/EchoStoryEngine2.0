package io.github.kxyr0.echonewui.forge.render;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kxyr0.echonewui.core.layout.UiRectangle;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnosticCode;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class JournalBookRenderer {
    public static final int SOURCE_WIDTH = 196;
    public static final int SOURCE_HEIGHT = 153;

    public void render(GuiGraphics graphics, ResourceLocation texture, UiRectangle rect,
                       NewUiDiagnostics diagnostics) {
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) {
            diagnostics.warn(NewUiDiagnosticCode.NEW_UI_TEXTURE_MISSING, texture.toString());
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        var textureObject = Minecraft.getInstance().getTextureManager().getTexture(texture);
        textureObject.setFilter(false, false);
        try {
            graphics.blit(texture, rect.x(), rect.y(), rect.width(), rect.height(),
                    0.0F, 0.0F, SOURCE_WIDTH, SOURCE_HEIGHT, SOURCE_WIDTH, SOURCE_HEIGHT);
        } finally {
            textureObject.setFilter(false, false);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }
}
