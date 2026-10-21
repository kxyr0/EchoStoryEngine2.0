package io.github.kxyr0.echoanimationauthoring.export;

import io.github.kxyr0.echoanimationauthoring.workspace.WorkspaceBoundary;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * Exports a 64x64 neutral NPC skin from ordered face layers.
 */
public final class NeutralSkinExporter {
    private final WorkspaceBoundary boundary = new WorkspaceBoundary();

    public Path export(Path workspace, Path output, List<LayerImage> layers) throws IOException {
        Path safeOutput = boundary.requireInside(Optional.of(workspace), output);
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (LayerImage layer : layers) {
            if (layer.visible()) {
                copyExact(layer, skin);
            }
        }
        Files.createDirectories(safeOutput.getParent());
        ImageIO.write(skin, "png", safeOutput.toFile());
        return safeOutput;
    }

    private void copyExact(LayerImage layer, BufferedImage target) {
        requireFits(layer);
        for (int y = 0; y < layer.image().getHeight(); y++) {
            for (int x = 0; x < layer.image().getWidth(); x++) {
                int targetX = layer.x() + x;
                int targetY = layer.y() + y;
                target.setRGB(targetX, targetY, layer.image().getRGB(x, y));
            }
        }
    }

    private void requireFits(LayerImage layer) {
        int maxX = layer.x() + layer.image().getWidth();
        int maxY = layer.y() + layer.image().getHeight();
        if (maxX > 64 || maxY > 64) {
            throw new IllegalArgumentException("Face layer выходит за пределы 64x64 skin atlas.");
        }
    }
}
