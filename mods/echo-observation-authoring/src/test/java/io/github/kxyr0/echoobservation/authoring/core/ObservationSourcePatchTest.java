package io.github.kxyr0.echoobservation.authoring.core;

import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.api.model.ObservationFace;
import io.github.kxyr0.echoobservation.echoscript.ObservationResourceCompiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationSourcePatchTest {
    private final ObservationSourcePatch patch = new ObservationSourcePatch();

    @Test
    void insertChangesOnlyObservationBodyPrefix() {
        String before = """
                class X : ObservationResource
                {
                    Observation BrokenWindow()
                    {
                        Focus(1.2);

                        Text("Стекло разбито изнутри.");
                    }
                }
                """;
        String after = patch.place(before, definition(before), anchor(120)).candidate().orElseThrow();
        assertTrue(after.contains("At(\n            Block(120, 64, -53),"));
        assertTrue(after.contains("\n\n        Focus(1.2);"));
        assertEquals(before.substring(before.indexOf("Focus")), after.substring(after.indexOf("Focus")));
    }

    @Test
    void repositionReplacesOneAtAndClearRemovesOnlyAt() {
        String unplaced = "class X : ObservationResource { Observation A() { Focus(1.0); } }";
        String placed = patch.place(unplaced, definition(unplaced), anchor(1)).candidate().orElseThrow();
        String moved = patch.place(placed, definition(placed), anchor(2)).candidate().orElseThrow();
        assertEquals(1, moved.split("At\\(", -1).length - 1);
        assertTrue(moved.contains("Block(2, 64, -53)"));
        String cleared = patch.clear(moved, definition(moved)).candidate().orElseThrow();
        assertFalse(cleared.contains("At("));
        assertTrue(cleared.contains("Focus(1.0);"));
    }

    @Test
    void placementAcceptsRaycastPrecisionRoundedInSource() {
        String source = "class X : ObservationResource { Observation A() { Focus(1.0); } }";
        ObservationAnchor raycast = new ObservationAnchor("minecraft:overworld", 2, -2, 415,
                0.347281D, 0.618719D, 0.0D, ObservationFace.WEST);

        String candidate = patch.place(source, definition(source), raycast).candidate().orElseThrow();

        assertTrue(candidate.contains("Offset(0.347, 0.619, 0.000)"));
    }

    @Test
    void sourcePathCannotEscapeDeveloperContentRoot() {
        ObservationSourcePath paths = new ObservationSourcePath(java.nio.file.Path.of("game"));
        assertTrue(paths.resolve("jdh:echo_observation/observations/room.es").isPresent());
        assertTrue(paths.resolve("jdh:echo_observation/observations/../../../../outside.es").isEmpty());
        assertTrue(paths.resolve("jdh:stories/not_observation.es").isEmpty());
    }

    private io.github.kxyr0.echoobservation.api.model.ObservationDefinition definition(String source) {
        return new ObservationResourceCompiler().compile(
                "jdh:echo_observation/observations/test.es", source)
                .resource().orElseThrow().observations().get(0);
    }

    private ObservationAnchor anchor(int x) {
        return new ObservationAnchor("minecraft:overworld", x, 64, -53,
                0.721D, 0.438D, 1.0D, ObservationFace.SOUTH);
    }
}
