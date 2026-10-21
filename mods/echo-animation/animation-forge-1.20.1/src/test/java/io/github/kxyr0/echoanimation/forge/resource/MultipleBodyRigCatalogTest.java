package io.github.kxyr0.echoanimation.forge.resource;

import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class MultipleBodyRigCatalogTest {
    @Test
    void animationUsesItsMainClipsExplicitRigWhenCatalogHasSeveralRigs() {
        List<AnimationResourceText> resources = List.of(
                rig("test:a"), rig("test:b"),
                clip("test:a_enter", "test:a"), clip("test:a_main", "test:a"),
                clip("test:a_exit", "test:a"), clip("test:b_idle", "test:b"), animation());
        AnimationCatalogCandidate candidate = new AnimationResourceCandidateCompiler(
                new EchoScriptAnimationCompiler()).compile(resources);

        Assertions.assertTrue(candidate.valid(), candidate.diagnostics().toString());
        Assertions.assertEquals(2, candidate.resourceSet().animationRigs().size());
        Assertions.assertEquals(1, candidate.resourceSet().animations().size());
    }

    private AnimationResourceText rig(String id) {
        return new AnimationResourceText(id + ".animation-rig.es", """
                namespace Test;
                class Rig : AnimationRigResource
                {
                    public override AnimationRigDef Build()
                    {
                        return AnimationRig("%s", skin: "CLASSIC");
                    }
                }
                """.formatted(id));
    }

    private AnimationResourceText clip(String id, String rig) {
        return new AnimationResourceText(id + ".animation-clip.es", """
                namespace Test;
                class Clip : AnimationClipResource
                {
                    public override AnimationClipDef Build()
                    {
                        return AnimationClip("%s", rig: "%s", tracks: [
                            Track("right_forearm", frames: [
                                Frame("0.0").Rotation(0, 0, 0),
                                Frame("1.0").Rotation(0, 0, 0)
                            ])]);
                    }
                }
                """.formatted(id, rig));
    }

    private AnimationResourceText animation() {
        return new AnimationResourceText("a.animation.es", """
                namespace Test;
                class Action : AnimationResource
                {
                    public override AnimationDef Build()
                    {
                        return Animation("test:a_action",
                            channels: [Channel("body")], sources: [Source("body/idle")],
                            entries: [Entry("body/idle", clip: "test:a_enter", durationTicks: 1)],
                            main: Main(clip: "test:a_main", mode: "LOOP_UNTIL_STOP", durationTicks: 20,
                                exitPoints: [ExitPoint("stop", progress: "0.0")]),
                            exits: [Exit("stop", clip: "test:a_exit", durationTicks: 1)],
                            target: "body/idle", recovery: Recovery("BOUNDED", checkpointIntervalTicks: 2));
                    }
                }
                """);
    }
}
