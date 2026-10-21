package io.github.kxyr0.echoanimation.forge.client;

import net.minecraft.resources.ResourceLocation;

/**
 * Converts authored face texture ids to Minecraft client texture resources.
 */
public final class FaceLayerTextureLocations {
    private FaceLayerTextureLocations() {
    }

    public static ResourceLocation clientTexture(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            return null;
        }
        String path = normalizePath(id.getPath());
        if (path.isBlank()) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/" + path + ".png");
    }

    private static String normalizePath(String value) {
        String path = value.replace('\\', '/');
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
