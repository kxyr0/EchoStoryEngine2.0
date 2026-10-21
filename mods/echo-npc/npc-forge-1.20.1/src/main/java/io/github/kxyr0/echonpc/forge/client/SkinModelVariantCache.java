package io.github.kxyr0.echonpc.forge.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

/**
 * Infers vanilla player model arm width from skin transparency.
 */
final class SkinModelVariantCache {
    private static final int MAX_CACHE_ENTRIES = 128;
    private static final int MIN_TRANSPARENT_SLIM_ARM_PIXELS = 8;
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<ResourceLocation, SkinModelVariant> variants = new LinkedHashMap<>(16, 0.75F, true);

    SkinModelVariant variant(ResourceLocation texture) {
        SkinModelVariant cached = variants.get(texture);
        if (cached != null) {
            return cached;
        }
        SkinModelVariant resolved = readVariant(texture);
        variants.put(texture, resolved);
        trim();
        return resolved;
    }

    void clear() {
        variants.clear();
    }

    private SkinModelVariant readVariant(ResourceLocation texture) {
        return Minecraft.getInstance().getResourceManager().getResource(texture)
                .map(resource -> readImageVariant(texture, resource))
                .orElse(SkinModelVariant.CLASSIC);
    }

    private SkinModelVariant readImageVariant(ResourceLocation texture, Resource resource) {
        try (InputStream input = resource.open(); NativeImage image = NativeImage.read(input)) {
            if (image.getWidth() != 64 || image.getHeight() != 64) {
                return SkinModelVariant.CLASSIC;
            }
            return hasSlimArmMask(image) ? SkinModelVariant.SLIM : SkinModelVariant.CLASSIC;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("ENPC-SKIN-VARIANT-READ-FAILED texture={} fallback=CLASSIC", texture, exception);
            return SkinModelVariant.CLASSIC;
        }
    }

    private boolean hasSlimArmMask(NativeImage image) {
        int right = transparentPixels(image, 54, 20, 56, 32);
        int left = transparentPixels(image, 46, 52, 48, 64);
        return Math.max(right, left) >= MIN_TRANSPARENT_SLIM_ARM_PIXELS;
    }

    private int transparentPixels(NativeImage image, int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (Byte.toUnsignedInt(image.getLuminanceOrAlpha(x, y)) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private void trim() {
        while (variants.size() > MAX_CACHE_ENTRIES) {
            variants.remove(variants.keySet().iterator().next());
        }
    }
}
