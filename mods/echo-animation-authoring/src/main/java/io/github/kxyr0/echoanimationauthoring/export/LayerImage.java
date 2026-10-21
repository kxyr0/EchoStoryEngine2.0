package io.github.kxyr0.echoanimationauthoring.export;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * One pixel-authored layer for neutral skin export.
 */
public record LayerImage(BufferedImage image, int x, int y, boolean visible) {
    public LayerImage {
        image = Objects.requireNonNull(image, "image");
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Layer position не может быть отрицательной.");
        }
    }
}

