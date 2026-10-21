package io.github.kxyr0.echoanimation.forge.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Reads texture dimensions for face-layer UV mapping.
 */
public final class FaceLayerTextureCache {
    private static final TextureSize FALLBACK_SIZE = new TextureSize(64, 64);

    private final Map<ResourceLocation, TextureSize> sizes = new HashMap<>();

    public TextureSize size(ResourceLocation texture) {
        return sizes.computeIfAbsent(texture, this::readSize);
    }

    public void clear() {
        sizes.clear();
    }

    private TextureSize readSize(ResourceLocation texture) {
        return Minecraft.getInstance().getResourceManager().getResource(texture)
                .map(this::readImageSize)
                .orElse(FALLBACK_SIZE);
    }

    private TextureSize readImageSize(net.minecraft.server.packs.resources.Resource resource) {
        try (InputStream input = resource.open(); NativeImage image = NativeImage.read(input)) {
            return new TextureSize(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()));
        } catch (IOException exception) {
            return FALLBACK_SIZE;
        }
    }

    public record TextureSize(int width, int height) {
    }
}
