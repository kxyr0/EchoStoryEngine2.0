package io.github.kxyr0.echoanimation.echoscript.procedural;

import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationRootMotion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ProceduralAnimationDslTest {
    private final EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();

    @Test
    void proceduralClipBuildsOneTimelineWithTypedSymbolsAndChains() {
        var result = compiler.compile("run.animation-clip.es", clip());

        Assertions.assertTrue(result.animationClip().isPresent(), result.diagnostics().toString());
        var clip = result.animationClip().orElseThrow();
        Assertions.assertEquals(17, clip.durationTicks());
        Assertions.assertEquals(3, clip.segments().size());
        Assertions.assertEquals(2, clip.segment("run_loop").orElseThrow().startTick());
        Assertions.assertEquals(4.3D, clip.segment("run_loop").orElseThrow()
                .authoredSpeedBlocksPerSecond(), 0.0001D);
        Assertions.assertEquals(2, clip.point("neutral").orElseThrow().tick());
        Assertions.assertEquals(9, clip.point("impact").orElseThrow().tick());
        Assertions.assertEquals("left", clip.events().get(0).payload());
        Assertions.assertEquals(AnimationRootMotion.Mode.EXTRACT, clip.rootMotion().mode());
        Assertions.assertEquals(1.0D, clip.tracks().get(0).keyframes().get(2).progress(), 0.0001D);
    }

    @Test
    void proceduralAnimationKeepsNativeSegmentAndPointReferences() {
        var result = compiler.compile("run.animation.es", animation());

        Assertions.assertTrue(result.animationDefinition().isPresent(), result.diagnostics().toString());
        var animation = result.animationDefinition().orElseThrow();
        Assertions.assertEquals("idle_to_run", animation.entryVariants().get(0).segmentId().orElseThrow());
        Assertions.assertEquals("run_loop", animation.main().segmentId().orElseThrow());
        Assertions.assertEquals("neutral", animation.main().exitPoints().get(0).sourcePointId().orElseThrow());
        Assertions.assertEquals("run_to_idle", animation.exitVariants().get(0).segmentId().orElseThrow());
        Assertions.assertEquals(3, animation.blendPolicy().fallbackCrossfadeTicks());
        Assertions.assertEquals(3, animation.blendPolicy().blendInTicks());
        Assertions.assertEquals(3, animation.blendPolicy().blendOutTicks());
        Assertions.assertEquals("upper_body", animation.blendPolicy().maskId().orElseThrow());
        Assertions.assertEquals("echo_item_attachments:attach_right_hand",
                animation.markers().get(0).id().value());
        Assertions.assertEquals(8L, animation.markers().get(0).phaseTick());
    }

    @Test
    void loopOnlyAndOneShotDoNotRequireSyntheticTransitions() {
        var loop = compiler.compile("idle.animation.es", simple("Loop(idle, play: motion);"));
        var once = compiler.compile("wave.animation.es", simple("OneShot(body, play: motion);"));

        Assertions.assertTrue(loop.animationDefinition().isPresent(), loop.diagnostics().toString());
        Assertions.assertTrue(loop.animationDefinition().orElseThrow().entryVariants().isEmpty());
        Assertions.assertTrue(loop.animationDefinition().orElseThrow().exitVariants().isEmpty());
        Assertions.assertTrue(once.animationDefinition().isPresent(), once.diagnostics().toString());
    }

    @Test
    void legacyWarnsAndMixedBuildIsRejected() {
        var legacy = compiler.compile("legacy.animation-clip.es", """
                class Legacy : AnimationClipResource {
                    public override AnimationClipDef Build() {
                        return AnimationClip("jdh:legacy", rig: "jdh:humanoid", tracks: []);
                    }
                }
                """);
        var mixed = compiler.compile("mixed.animation.es", """
                class Mixed : AnimationResource {
                    public override AnimationDef Build() {
                        Id("jdh:mixed");
                        return Animation("jdh:mixed");
                    }
                }
                """);

        Assertions.assertTrue(legacy.diagnostics().stream().anyMatch(d -> d.code().equals("EANIM-ES-LEGACY")));
        Assertions.assertTrue(mixed.diagnostics().stream().anyMatch(d -> d.code().equals("EANIM-ES-MIXED-BUILD")));
    }

    private String simple(String main) {
        return """
                class Simple : AnimationResource {
                    public override void Build() {
                        Id("jdh:simple");
                        var body = Channel("body");
                        var idle = State(body, "idle");
                        var motion = Clip("jdh:simple");
                        %s
                    }
                }
                """.formatted(main);
    }

    private String animation() {
        return """
                class Run : AnimationResource {
                    public override void Build() {
                        Id("jdh:run");
                        var body = Channel("body");
                        var idle = State(body, "idle");
                        var run = State(body, "run");
                        var motion = Clip("jdh:run");
                        Enter(run, from: idle, play: motion.Segment("idle_to_run"));
                        Loop(run, play: motion.Segment("run_loop"));
                        Exit(run, to: idle, play: motion.Segment("run_to_idle"), at: motion.Point("neutral"));
                        Fallback(body, Crossfade(3));
                        BlendIn(3);
                        BlendOut(3);
                        Mask(body, "upper_body");
                        Marker("echo_item_attachments:attach_right_hand", phase: MAIN, tick: 8);
                        Recovery(Bounded, checkpointEvery: 3);
                        Replay(RejectIfRunning);
                    }
                }
                """;
    }

    private String clip() {
        return """
                class RunClip : AnimationClipResource {
                    public override void Build() {
                        Id("jdh:run");
                        Rig("jdh:humanoid");
                        Duration(17);
                        var enter = Segment("idle_to_run", from: 0, to: 2);
                        var loop = Segment("run_loop", from: 2, to: 14);
                        var exit = Segment("run_to_idle", from: 14, to: 17);
                        Loop(loop, seam: Smooth);
                        Speed(loop, blocksPerSecond: 4.3);
                        var neutral = Point(loop, "neutral", at: Start);
                        Point("impact", at: 9);
                        Event("footstep", at: 5, payload: "left");
                        RootMotion(Extract, bone: "root");
                        var body = Track("body");
                        Key(body, 0).Position(0, 12, 0).Rotation(0, 0, 0);
                        Key(body, 2, Cubic).Position(0, 11.7, 0).Rotation(-8, 0, 0);
                        Key(body, 17).Position(0, 12, 0).Rotation(0, 0, 0);
                    }
                }
                """;
    }
}
