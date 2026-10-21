package io.github.kxyr0.echowardrobe.core.asset;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Scans wardrobe PNG assets for authoring inventory reports.
 */
public final class WardrobeAssetInventoryScanner {
    public List<PngAssetRecord> scan(Path assetRoot, String namespace) throws IOException {
        if (!Files.isDirectory(assetRoot)) {
            return List.of();
        }
        try (var stream = Files.walk(assetRoot)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".png"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(path -> record(assetRoot, namespace, path))
                    .toList();
        }
    }

    private PngAssetRecord record(Path assetRoot, String namespace, Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            Path relative = assetRoot.relativize(path);
            String id = resourceId(namespace, relative);
            return new PngAssetRecord(path, id, image.getWidth(), image.getHeight(), image.getColorModel().hasAlpha(),
                    sha256(path), category(relative), false);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read PNG asset: " + path, exception);
        }
    }

    private String resourceId(String namespace, Path relative) {
        String path = relative.toString().replace('\\', '/');
        String withoutExtension = path.substring(0, path.length() - ".png".length());
        if (withoutExtension.startsWith("textures/")) {
            withoutExtension = withoutExtension.substring("textures/".length());
        }
        return namespace + ":" + withoutExtension;
    }

    private String category(Path relative) {
        String path = relative.toString().replace('\\', '/');
        if (path.contains("/textures/") || path.contains("wearables/textures/")) {
            return "accessory-or-model-texture";
        }
        if (path.contains("wearables/")) {
            return "candidate-full-outfit-skin";
        }
        return "unknown";
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                input.transferTo(new java.io.OutputStream() {
                    @Override
                    public void write(int value) {
                        digest.update((byte) value);
                    }

                    @Override
                    public void write(byte[] bytes, int offset, int length) {
                        digest.update(bytes, offset, length);
                    }
                });
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
