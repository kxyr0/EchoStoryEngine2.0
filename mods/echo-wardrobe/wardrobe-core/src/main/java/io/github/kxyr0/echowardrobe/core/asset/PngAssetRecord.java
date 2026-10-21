package io.github.kxyr0.echowardrobe.core.asset;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Single PNG asset inventory entry.
 */
public record PngAssetRecord(
        Path path,
        String resourceId,
        int width,
        int height,
        boolean hasAlpha,
        String sha256,
        String likelyCategory,
        boolean confirmedOutfitMapping
) {
    public PngAssetRecord {
        path = Objects.requireNonNull(path, "path");
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
        sha256 = Objects.requireNonNull(sha256, "sha256");
        likelyCategory = Objects.requireNonNull(likelyCategory, "likelyCategory");
    }

    public boolean minecraftSkin64() {
        return width == 64 && height == 64;
    }
}
