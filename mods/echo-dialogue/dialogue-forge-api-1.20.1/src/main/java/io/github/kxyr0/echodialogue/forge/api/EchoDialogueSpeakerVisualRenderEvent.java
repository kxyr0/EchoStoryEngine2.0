package io.github.kxyr0.echodialogue.forge.api;

import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

/**
 * Client-side hook fired after Echo Dialogue draws one speaker visual.
 */
public final class EchoDialogueSpeakerVisualRenderEvent extends Event {
    private final GuiGraphics graphics;
    private final SpeakerVisualKind kind;
    private final String visualId;
    private final ResourceLocation texture;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public EchoDialogueSpeakerVisualRenderEvent(
            GuiGraphics graphics,
            SpeakerVisualKind kind,
            String visualId,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height
    ) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.visualId = Objects.requireNonNull(visualId, "visualId");
        this.texture = Objects.requireNonNull(texture, "texture");
        this.x = x;
        this.y = y;
        this.width = requirePositive(width, "width");
        this.height = requirePositive(height, "height");
    }

    public GuiGraphics graphics() {
        return graphics;
    }

    public SpeakerVisualKind kind() {
        return kind;
    }

    public String visualId() {
        return visualId;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " должен быть положительным.");
        }
        return value;
    }
}
