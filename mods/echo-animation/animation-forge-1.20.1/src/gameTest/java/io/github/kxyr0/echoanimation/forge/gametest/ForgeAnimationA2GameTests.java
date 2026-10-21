package io.github.kxyr0.echoanimation.forge.gametest;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationGraphId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationChannelDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationGraphDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationTransitionDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.InterruptionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.core.runtime.AnimationLease;
import io.github.kxyr0.echoanimation.core.runtime.AnimationSubjectRuntime;
import io.github.kxyr0.echoanimation.core.sync.AnimationSubjectSyncTracker;
import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import io.github.kxyr0.echoanimation.forge.EchoAnimationMod;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogCandidate;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceCandidateCompiler;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceText;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import org.slf4j.Logger;

@GameTestHolder(EchoAnimationMod.MOD_ID)
public final class ForgeAnimationA2GameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AnimationChannelId BODY = new AnimationChannelId("body");
    private static final AnimationStateId BODY_IDLE = new AnimationStateId("body/idle");
    private static final AnimationStateId BODY_WAVE = new AnimationStateId("body/wave");

    private ForgeAnimationA2GameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void twoSubjectsShareGraphWithIndependentStates(GameTestHelper helper) {
        mark("ForgeAnimationA2GameTests.twoSubjectsShareGraphWithIndependentStates");
        AnimationGraphDefinition graph = bodyGraph();
        AnimationSubjectRuntime first = new AnimationSubjectRuntime(new AnimationSubjectId("jdh:first"), graph);
        AnimationSubjectRuntime second = new AnimationSubjectRuntime(new AnimationSubjectId("jdh:second"), graph);
        AnimationLease lease = first.acquire(BODY, new AnimationOwnerId("test:story")).lease().orElseThrow();

        first.signal(lease, "wave");
        first.tick(7L);
        second.tick(2L);

        helper.assertTrue(first.snapshot().channels().get(BODY).stateId().equals(BODY_WAVE),
                "first subject must enter wave");
        helper.assertTrue(second.snapshot().channels().get(BODY).stateId().equals(BODY_IDLE),
                "second subject must remain idle");
        helper.assertTrue(first.snapshot().channels().get(BODY).elapsedTicks() == 7L,
                "first subject must own its playback cursor");
        helper.assertTrue(second.snapshot().channels().get(BODY).elapsedTicks() == 2L,
                "second subject must own its playback cursor");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void trackingLifecyclePublishesFullDeltaAndRemoval(GameTestHelper helper) {
        mark("ForgeAnimationA2GameTests.trackingLifecyclePublishesFullDeltaAndRemoval");
        AnimationGraphDefinition graph = bodyGraph();
        AnimationSubjectRuntime runtime = new AnimationSubjectRuntime(new AnimationSubjectId("jdh:actor"), graph);
        AnimationSubjectSyncTracker tracker = new AnimationSubjectSyncTracker();
        var full = tracker.startTracking("player-a", graph, runtime.snapshot(), 100L);
        AnimationLease lease = runtime.acquire(BODY, new AnimationOwnerId("test:story")).lease().orElseThrow();

        runtime.signal(lease, "wave");
        var deltas = tracker.publishChange(graph, runtime.snapshot(), 101L);
        var removal = tracker.stopTracking("player-a", runtime.snapshot().subjectId());
        runtime.tick(3L);

        helper.assertTrue(full.full(), "tracking must start with full snapshot");
        helper.assertTrue(deltas.size() == 1 && !deltas.get(0).full(), "state change must publish one delta");
        helper.assertTrue(deltas.get(0).snapshot().channels().get(0).previousClipId()
                .equals(Optional.of(AnimationClipId.parse("jdh:idle"))), "delta must carry transition source");
        helper.assertTrue(removal.isPresent(), "stop tracking must return removal packet data");
        helper.assertTrue(tracker.publishChange(graph, runtime.snapshot(), 104L).isEmpty(),
                "stopped subscriber must not receive updates");
        helper.assertTrue(tracker.startTracking("player-a", graph, runtime.snapshot(), 105L)
                .snapshot().channels().get(0).stateId().equals(BODY_WAVE),
                "retracking must send current state");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void invalidBodyClipRetainsKnownGoodCatalog(GameTestHelper helper) {
        mark("ForgeAnimationA2GameTests.invalidBodyClipRetainsKnownGoodCatalog");
        AnimationCatalogState state = new AnimationCatalogState();
        AnimationResourceCandidateCompiler compiler =
                new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler());

        AnimationCatalogCandidate valid = compiler.compile(List.of(
                bodyRig(), bodyClip("right_forearm", 450)));
        state.install(valid);
        AnimationCatalogCandidate invalid = compiler.compile(List.of(
                bodyRig(), bodyClip("right_hand", 450)));
        state.install(invalid);

        helper.assertTrue(valid.valid(), "valid body rig and clip must install");
        helper.assertTrue(!invalid.valid(), "unknown bone track must reject the candidate");
        helper.assertTrue(state.current().animationClips().size() == 1,
                "known-good body clip catalog must be retained");
        helper.assertTrue(state.current().animationClips().get(0).id().equals(AnimationClipId.parse("jdh:wave")),
                "retained catalog must still expose previous wave clip");
        helper.succeed();
    }

    private static AnimationGraphDefinition bodyGraph() {
        return new AnimationGraphDefinition(new AnimationGraphId("jdh:humanoid_graph"), 1L,
                List.of(new AnimationChannelDefinition(BODY, BODY_IDLE, 10, ChannelCompositionPolicy.OVERRIDE,
                        Set.of(BoneId.parse("right_forearm")))),
                List.of(bodyState(BODY_IDLE, "jdh:idle", PlaybackMode.LOOP),
                        bodyState(BODY_WAVE, "jdh:wave", PlaybackMode.HOLD_LAST_FRAME)),
                List.of(new AnimationTransitionDefinition(BODY, BODY_IDLE, "wave", BODY_WAVE, 0, 5L)));
    }

    private static AnimationStateDefinition bodyState(AnimationStateId id, String clipId, PlaybackMode mode) {
        return new AnimationStateDefinition(id, BODY, 20L, mode, InterruptionPolicy.IMMEDIATE,
                Optional.of(AnimationClipId.parse(clipId)), 1.0D);
    }

    private static AnimationResourceText bodyRig() {
        return new AnimationResourceText("jdh/humanoid.animation-rig.es",
                ForgeAnimationGameTestSources.bodyRig());
    }

    private static AnimationResourceText bodyClip(String boneId, int forearmRoll) {
        return new AnimationResourceText("jdh/wave.animation-clip.es",
                ForgeAnimationGameTestSources.bodyClip(boneId, forearmRoll));
    }

    private static void mark(String testName) {
        LOGGER.info("EANIM-GAMETEST-RUN {}", testName);
    }
}
