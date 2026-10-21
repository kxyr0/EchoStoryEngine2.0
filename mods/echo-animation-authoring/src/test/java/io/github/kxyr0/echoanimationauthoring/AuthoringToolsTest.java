package io.github.kxyr0.echoanimationauthoring;

import io.github.kxyr0.echoanimationauthoring.editor.AuthoringLabels;
import io.github.kxyr0.echoanimationauthoring.editor.EditorSession;
import io.github.kxyr0.echoanimationauthoring.export.LayerImage;
import io.github.kxyr0.echoanimationauthoring.export.NeutralSkinExporter;
import io.github.kxyr0.echoanimationauthoring.migration.LegacyFaceMigration;
import io.github.kxyr0.echoanimationauthoring.workspace.WorkspaceBoundary;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AuthoringToolsTest {
    @TempDir
    Path temp;

    @Test
    void russianLabelsAreVerified() {
        Assertions.assertDoesNotThrow(AuthoringLabels::requireRussianLabels);
    }

    @Test
    void workspaceRejectsWritesWhenUnconfigured() {
        var boundary = new WorkspaceBoundary();
        Assertions.assertThrows(IllegalStateException.class, () -> boundary.requireInside(Optional.empty(), temp));
    }

    @Test
    void editorSessionClampsTimelineAndBlocksUnconfiguredWrites() {
        EditorSession session = EditorSession.unconfigured().selectTool(AuthoringLabels.PIVOT).scrubTimeline(999);
        Assertions.assertFalse(session.writeEnabled());
        Assertions.assertEquals(AuthoringLabels.PIVOT, session.selectedTool());
        Assertions.assertEquals(EditorSession.MAX_TIMELINE_FRAME, session.timelineFrame());
        Assertions.assertEquals(AuthoringLabels.WRITES_BLOCKED, session.writeStateLabel());
        Assertions.assertFalse(session.hasGlasses());
    }

    @Test
    void glassesAuthoringCapabilityRequiresExplicitConfiguration() {
        EditorSession session = EditorSession.unconfigured();
        Assertions.assertFalse(session.hasGlasses());
        Assertions.assertTrue(session.withGlassesConfiguration(true).hasGlasses());
    }

    @Test
    void workspaceRejectsEscapingOutput() {
        var boundary = new WorkspaceBoundary();
        Path outside = temp.resolveSibling("outside").resolve("skin.png");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> boundary.requireInside(Optional.of(temp), outside));
    }

    @Test
    void legacyMigrationMapsKnownFieldsAndWarnsUnresolvedFields() {
        String source = """
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
        var result = new LegacyFaceMigration().migrate(source, "jdh:shared/blink", "jdh:john_father");
        Assertions.assertTrue(result.echoScript().contains("durationMillis: 1000"));
        Assertions.assertTrue(result.echoScript().contains("end: \"HOLD\""));
        Assertions.assertTrue(result.echoScript().contains("x: 70, y: 300"));
        Assertions.assertTrue(result.echoScript().contains("y: -100"));
        Assertions.assertEquals(2, result.warnings().size());
    }

    @Test
    void neutralSkinExportWritesExactPixelInsideWorkspace() throws Exception {
        BufferedImage layer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        layer.setRGB(0, 0, 0xFFFF0000);
        Path output = temp.resolve("generated").resolve("john_neutral.png");
        new NeutralSkinExporter().export(temp, output, List.of(new LayerImage(layer, 8, 8, true)));
        BufferedImage exported = ImageIO.read(output.toFile());
        Assertions.assertEquals(64, exported.getWidth());
        Assertions.assertEquals(64, exported.getHeight());
        Assertions.assertEquals(0xFFFF0000, exported.getRGB(8, 8));
    }
}
