package io.github.kxyr0.echodialogue.forge.client.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueSpeakerVisualRenderEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.common.MinecraftForge;

/**
 * Draws resolved speaker visuals inside an already calculated GUI rectangle.
 */
public final class ClientSpeakerVisualRenderer {
    private static final int SKIN_TEXTURE_SIZE = 64;
    private static final int SKIN_HEAD_U = 8;
    private static final int SKIN_HEAD_V = 8;
    private static final int SKIN_HEAD_SIZE = 8;
    private static final int SKIN_HEAD_OUTER_U = 40;

    private ClientSpeakerVisualRenderer() {
    }

    public static void render(GuiGraphics graphics, ClientSpeakerVisual visual, int x, int y, int size) {
        render(graphics, visual, new RenderPlacement(x, y, size, 1.0F));
    }

    public static void render(GuiGraphics graphics, ClientSpeakerVisual visual, RenderPlacement placement) {
        if (rendersAsSkinHead(visual)) {
            renderSkinHead(graphics, visual, placement);
        } else {
            renderPortrait(graphics, visual, placement);
        }
    }

    static boolean rendersAsSkinHead(ClientSpeakerVisual visual) {
        return visual.kind() == SpeakerVisualKind.SKIN_HEAD || visual.skinSheet();
    }

    private static void renderPortrait(GuiGraphics graphics, ClientSpeakerVisual visual, RenderPlacement placement) {
        float scale = Math.min((float) placement.size() / visual.sourceWidth(),
                (float) placement.size() / visual.sourceHeight());
        int width = Math.max(1, Math.round(visual.sourceWidth() * scale));
        int height = Math.max(1, Math.round(visual.sourceHeight() * scale));
        int drawX = placement.x() + (placement.size() - width) / 2;
        int drawY = placement.y() + (placement.size() - height) / 2;
        renderWithAlpha(placement.alpha(), () -> graphics.blit(
                visual.texture(), drawX, drawY, width, height, 0.0F, 0.0F,
                visual.sourceWidth(), visual.sourceHeight(), visual.sourceWidth(), visual.sourceHeight()));
        postVisualRendered(graphics, visual, new VisualBounds(drawX, drawY, width, height));
    }

    private static void renderSkinHead(GuiGraphics graphics, ClientSpeakerVisual visual, RenderPlacement placement) {
        renderWithAlpha(placement.alpha(), () -> {
            graphics.blit(visual.texture(), placement.x(), placement.y(), placement.size(), placement.size(),
                    SKIN_HEAD_U, SKIN_HEAD_V,
                    SKIN_HEAD_SIZE, SKIN_HEAD_SIZE, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE);
            VisualBounds outer = skinHeadOuterBounds(placement.x(), placement.y(), placement.size());
            renderSkinHeadOuter(graphics, visual, outer);
        });
        postVisualRendered(graphics, visual,
                new VisualBounds(placement.x(), placement.y(), placement.size(), placement.size()));
    }

    private static void renderSkinHeadOuter(GuiGraphics graphics, ClientSpeakerVisual visual, VisualBounds outer) {
        graphics.blit(visual.texture(), outer.x(), outer.y(), outer.width(), outer.height(),
                SKIN_HEAD_OUTER_U, SKIN_HEAD_V, SKIN_HEAD_SIZE, SKIN_HEAD_SIZE,
                SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE);
    }

    private static VisualBounds skinHeadOuterBounds(int x, int y, int size) {
        int inflate = Math.max(1, Math.round(size / 16.0F));
        return new VisualBounds(x - inflate, y - inflate, size + inflate * 2, size + inflate * 2);
    }

    static float effectiveAlpha(float alpha) {
        return Math.max(0.0F, Math.min(1.0F, alpha));
    }

    private static void renderWithAlpha(float alpha, Runnable renderer) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, effectiveAlpha(alpha));
        try {
            renderer.run();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private static void postVisualRendered(GuiGraphics graphics, ClientSpeakerVisual visual, VisualBounds bounds) {
        MinecraftForge.EVENT_BUS.post(new EchoDialogueSpeakerVisualRenderEvent(
                graphics, visual.kind(), visual.id().toString(), visual.texture(),
                bounds.x(), bounds.y(), bounds.width(), bounds.height()));
    }

    public record RenderPlacement(int x, int y, int size, float alpha) {
    }

    private record VisualBounds(int x, int y, int width, int height) {
    }
}
