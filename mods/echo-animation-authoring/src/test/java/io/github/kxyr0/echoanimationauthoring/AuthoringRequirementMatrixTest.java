package io.github.kxyr0.echoanimationauthoring;

import io.github.kxyr0.echoanimationauthoring.export.LayerImage;
import io.github.kxyr0.echoanimationauthoring.export.NeutralSkinExporter;
import io.github.kxyr0.echoanimationauthoring.migration.LegacyFaceMigration;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AuthoringRequirementMatrixTest {
    @TempDir
    Path temp;

    @Test
    void lengthSecondsMapsToDuration() {
        Assertions.assertTrue(migrated().echoScript().contains("durationMillis: 1000"));
    }

    @Test
    void endHoldMapsToHold() {
        Assertions.assertTrue(migrated().echoScript().contains("end: \"HOLD\""));
    }

    @Test
    void moveMapsToLocalTranslation() {
        Assertions.assertTrue(migrated().echoScript().contains("x: 70, y: 300"));
    }

    @Test
    void tiltProducesAuthorConfirmationWarning() {
        Assertions.assertTrue(migrated().warnings().stream().anyMatch(warning -> warning.contains("tilt")));
    }

    @Test
    void originalJson5RemainsUntouched() {
        String source = legacySource();
        new LegacyFaceMigration().migrate(source, "jdh:shared/blink", "jdh:john_father");
        Assertions.assertEquals(legacySource(), source);
    }

    @Test
    void migrationOutputIsDeterministic() {
        var first = migrated();
        var second = migrated();
        Assertions.assertEquals(first.echoScript(), second.echoScript());
        Assertions.assertEquals(first.warnings(), second.warnings());
    }

    @Test
    void blinkTravelYProducesWarning() {
        Assertions.assertTrue(migrated().warnings().stream().anyMatch(warning -> warning.contains("blinkTravelY")));
    }

    @Test
    void glassesAreIncludedAboveEyelids() throws Exception {
        Path output = temp.resolve("skin.png");
        new NeutralSkinExporter().export(temp, output, List.of(layer(0xFF00FF00, true), layer(0xFF0000FF, true)));
        Assertions.assertEquals(0xFF0000FF, ImageIO.read(output.toFile()).getRGB(8, 8));
    }

    @Test
    void hiddenEditorGlassesDoNotAlterExport() throws Exception {
        Path output = temp.resolve("skin.png");
        new NeutralSkinExporter().export(temp, output, List.of(layer(0xFF00FF00, true), layer(0xFF0000FF, false)));
        Assertions.assertEquals(0xFF00FF00, ImageIO.read(output.toFile()).getRGB(8, 8));
    }

    @Test
    void missingAtlasPlacementBlocksExport() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Path output = temp.resolve("skin.png");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new NeutralSkinExporter().export(temp, output, List.of(new LayerImage(image, 60, 60, true))));
    }

    private io.github.kxyr0.echoanimationauthoring.migration.MigrationResult migrated() {
        return new LegacyFaceMigration().migrate(legacySource(), "jdh:shared/blink", "jdh:john_father");
    }

    private LayerImage layer(int color, boolean visible) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, color);
        return new LayerImage(image, 8, 8, visible);
    }

    private String legacySource() {
        return """
                {
                    lengthSeconds: 1,
                    end: "hold",
                    blinkTravelY: { upperEyelids: 1, lowerEyelids: 1 },
                    parts: {
                        brows: {
                            left: { to: { tilt: 11.7, move: { x: 0.07, y: 0.3 } } },
                            right: { to: { tilt: -11.7, move: { x: -0.07, y: 0.3 } } }
                        },
                        upperEyelids: { to: 0.22 },
                        lowerEyelids: { to: -0.10 }
                    }
                }
                """;
    }
}
