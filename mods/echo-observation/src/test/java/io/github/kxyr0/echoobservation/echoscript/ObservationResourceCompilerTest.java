package io.github.kxyr0.echoobservation.echoscript;

import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnosticCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationResourceCompilerTest {
    private final ObservationResourceCompiler compiler = new ObservationResourceCompiler();

    @Test
    void acceptanceSourceIsParserAndBinderValidWithoutNamespace() {
        ObservationCompileResult result = compile("""
                class TestObservationScene : ObservationResource
                {
                    Observation BrokenWindow()
                    {
                        Focus(1.2);
                        Text("Стекло разбито изнутри.");
                        Thought("Окно разбили изнутри.");
                        OnComplete() {
                            Story.Start("jdh:test_observation_completed");
                            Dialogue.Load("jdh:broken_window_reaction");
                        }
                    }
                    Observation DoorLock() { Focus(1.0); Text("На замке нет следов взлома."); }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals("jdh:museum_room", result.resource().orElseThrow().id().toString());
        assertEquals("jdh:museum_room#broken_window",
                result.resource().orElseThrow().observations().get(0).id().toString());
        assertEquals("Story.Start", result.resource().orElseThrow().observations().get(0)
                .onComplete().actions().get(0).target());
        assertEquals("Dialogue.Load", result.resource().orElseThrow().observations().get(0)
                .onComplete().actions().get(1).target());
        assertEquals("Стекло разбито изнутри.", result.resource().orElseThrow().observations().get(0)
                .text().orElseThrow());
        assertEquals("Окно разбили изнутри.", result.resource().orElseThrow().observations().get(0)
                .thought().orElseThrow());
        assertTrue(result.resource().orElseThrow().observations().get(1).thought().isEmpty());
        assertFalse(compiler.productionValid(result));
    }

    @Test
    void placedAnchorIsTypedAndProductionValid() {
        ObservationCompileResult result = compile("""
                class X : ObservationResource {
                    Observation DoorLock() {
                        At(Block(120, 64, -53), Offset(0.721, 0.438, 1.000),
                            face: SOUTH, dimension: "minecraft:overworld");
                        Focus(1.0);
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var definition = result.resource().orElseThrow().observations().get(0);
        assertEquals(120.721D, definition.anchor().orElseThrow().worldX(), 0.0001D);
        assertTrue(definition.source().atSpan().isPresent());
        assertTrue(compiler.productionValid(result));
    }

    @Test
    void normalizedCollisionsAndInvalidOffsetsAreRejected() {
        ObservationCompileResult collision = compile("""
                class X : ObservationResource {
                    Observation FooBar() { Focus(1); }
                    Observation Foo_Bar() { Focus(1); }
                }
                """);
        assertFalse(collision.successful());
        assertTrue(collision.diagnostics().stream()
                .anyMatch(value -> value.code() == ObservationDiagnosticCode.OBSERVATION_ID_COLLISION));

        ObservationCompileResult offset = compile("""
                class X : ObservationResource {
                    Observation Bad() {
                        At(Block(0, 0, 0), Offset(1.1, 0, 0), face: UP,
                            dimension: "minecraft:overworld");
                    }
                }
                """);
        assertFalse(offset.successful());
    }

    @Test
    void checkedInAcceptanceResourcePassesRealParserAndBinder() throws IOException {
        try (var stream = getClass().getResourceAsStream(
                "/data/jdh/echo_observation/observations/test_scene.es")) {
            String source = new String(java.util.Objects.requireNonNull(stream).readAllBytes(),
                    StandardCharsets.UTF_8);
            ObservationCompileResult result = compiler.compile(
                    "jdh:echo_observation/observations/test_scene.es", source);
            assertTrue(result.successful(), () -> result.diagnostics().toString());
            assertEquals(2, result.resource().orElseThrow().observations().size());
        }
    }

    private ObservationCompileResult compile(String source) {
        return compiler.compile("jdh:echo_observation/observations/museum_room.es", source);
    }
}
