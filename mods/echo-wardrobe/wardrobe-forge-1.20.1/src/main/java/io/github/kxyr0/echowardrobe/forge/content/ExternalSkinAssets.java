package io.github.kxyr0.echowardrobe.forge.content;

import io.github.kxyr0.echowardrobe.core.asset.SkinAssetView;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 * File-system view of 64x64 wardrobe PNG assets from external content packs.
 */
public final class ExternalSkinAssets implements SkinAssetView {
    private final List<Path> contentPackRoots;
    private final Set<String> builtins;

    public ExternalSkinAssets(List<Path> contentPackRoots, Set<String> builtins) {
        this.contentPackRoots = List.copyOf(contentPackRoots);
        this.builtins = Set.copyOf(builtins);
    }

    @Override
    public boolean skinExists(String resourceId) {
        if (builtins.contains(resourceId)) {
            return true;
        }
        int separator = resourceId.indexOf(':');
        if (separator <= 0 || separator == resourceId.length() - 1) {
            return false;
        }
        String namespace = resourceId.substring(0, separator);
        String resourcePath = resourceId.substring(separator + 1);
        return contentPackRoots.stream()
                .map(root -> root.resolve("assets").resolve(namespace).resolve("textures")
                        .resolve(resourcePath + ".png"))
                .anyMatch(this::validSkin);
    }

    private boolean validSkin(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            return image != null && image.getWidth() == 64 && image.getHeight() == 64;
        } catch (IOException exception) {
            return false;
        }
    }
}
