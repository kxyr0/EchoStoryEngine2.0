package io.github.kxyr0.echovisibility.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.policy.VisibilityConcealmentPolicy;
import io.github.kxyr0.echovisibility.api.policy.VisibilityDetectionMode;
import io.github.kxyr0.echovisibility.api.target.VisibilityBoxTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import org.junit.jupiter.api.Test;

class EchoScriptVisibilityWatchCompilerTest {
    private final EchoScriptVisibilityWatchCompiler compiler = new EchoScriptVisibilityWatchCompiler();

    @Test
    void bindsInitOnlyNpcWatchThroughRealParser() {
        VisibilityWatchCompileResult result = compile(npcWatch());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var watch = result.definition().orElseThrow();
        assertEquals("jdh:home/father_vanish", watch.id().value());
        assertInstanceOf(VisibilityNpcTarget.class, watch.target());
        assertEquals(VisibilityDetectionMode.OUTSIDE_FRUSTUM_OR_OCCLUDED, watch.detectionMode());
        assertEquals(VisibilityConcealmentPolicy.HIDE_NPC_IMMEDIATELY, watch.concealmentPolicy());
        assertEquals(1, result.parserCalls());
        assertEquals(1, result.binderCalls());
    }

    @Test
    void bindsBoxWithNegativeCoordinates() {
        VisibilityWatchCompileResult result = compile(boxWatch());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        VisibilityBoxTarget target = assertInstanceOf(VisibilityBoxTarget.class,
                result.definition().orElseThrow().target());
        assertEquals(-8.5D, target.bounds().min().x());
        assertEquals("minecraft:overworld", target.dimension().value());
    }

    private VisibilityWatchCompileResult compile(String source) {
        return compiler.compile("test.es", source, new VisibilityDefinitionRevision(1L));
    }

    private static String npcWatch() {
        return """
                namespace JDH.Chapter1.Home.Visibility;
                class FatherVanishWatch : VisibilityWatchResource
                {
                    private VisibilityWatchId FatherVanish = "jdh:home/father_vanish";
                    private NpcId Father = "jdh:father";
                    private void Init()
                    {
                        Id(FatherVanish);
                        Target(Npc(Father));
                        Viewpoint(ACTIVE_CAMERA);
                        HiddenWhen(OUTSIDE_FRUSTUM_OR_OCCLUDED);
                        Samples(NPC_HEAD, NPC_CHEST, NPC_PELVIS, NPC_FEET, BOUNDS_CORNERS);
                        Occlusion(VISUAL_MULTI_RAY);
                        RequireVisibleFirst();
                        LostAfter(RenderedFrames(1));
                        SeenAfter(RenderedFrames(2));
                        EdgeMargin(Pixels(2));
                        Concealment(HIDE_NPC_IMMEDIATELY);
                        Evaluate(EVERY_RENDER_FRAME);
                    }
                }
                """;
    }

    private static String boxWatch() {
        return """
                namespace JDH.Chapter1.Home.Visibility;
                class KitchenEnvironmentWatch : VisibilityWatchResource
                {
                    private VisibilityWatchId Kitchen = "jdh:home/kitchen_change";
                    private void Init()
                    {
                        Id(Kitchen);
                        Target(Box(
                            dimension: "minecraft:overworld",
                            min: Position(-8.5, 60, -4),
                            max: Position(8, 72, 12)
                        ));
                        HiddenWhen(OUTSIDE_FRUSTUM_OR_OCCLUDED);
                        Samples(BOX_CENTER, BOX_CORNERS, BOX_FACE_CENTERS);
                        Occlusion(VISUAL_MULTI_RAY);
                        RequireVisibleFirst();
                        LostAfter(RenderedFrames(1));
                        SeenAfter(RenderedFrames(2));
                        EdgeMargin(Pixels(2));
                        Evaluate(EVERY_CLIENT_TICK);
                    }
                }
                """;
    }
}
