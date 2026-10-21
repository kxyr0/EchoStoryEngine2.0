package io.github.kxyr0.echoanimation.forge.resource;

import io.github.kxyr0.echoanimation.testsupport.TestSources;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationResourceCandidateCompilerTest {
    @Test
    void changedClipParsesAndBindsOnceWhileUnchangedResourcesStayCached() {
        EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();
        AnimationResourceCandidateCompiler candidates = new AnimationResourceCandidateCompiler(compiler);
        candidates.compile(List.of(rig(), clip("idle.animation-clip.es", "right_forearm", 0),
                clip("wave.animation-clip.es", "right_forearm", 450)));
        long parseCalls = compiler.parseCalls();
        long bindCalls = compiler.bindCalls();

        AnimationCatalogCandidate changed = candidates.compile(List.of(rig(),
                clip("idle.animation-clip.es", "right_forearm", 0),
                clip("wave.animation-clip.es", "right_forearm", 700)));

        Assertions.assertTrue(changed.valid());
        Assertions.assertEquals(1L, compiler.parseCalls() - parseCalls);
        Assertions.assertEquals(1L, compiler.bindCalls() - bindCalls);
    }

    @Test
    void unknownBodyClipTrackRejectsCandidateAndRetainsKnownGoodCatalog() {
        AnimationCatalogState state = new AnimationCatalogState();
        AnimationResourceCandidateCompiler candidates =
                new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler());
        AnimationCatalogCandidate valid = candidates.compile(List.of(rig(),
                clip("wave.animation-clip.es", "right_forearm", 450)));
        state.install(valid);

        AnimationCatalogCandidate invalid = candidates.compile(List.of(rig(),
                clip("wave.animation-clip.es", "right_hand", 450)));
        state.install(invalid);

        Assertions.assertTrue(valid.valid());
        Assertions.assertFalse(invalid.valid());
        Assertions.assertEquals(List.of(AnimationClipId.parse("jdh:wave")),
                state.current().animationClips().stream().map(clip -> clip.id()).toList());
        String message = invalid.diagnostics().stream()
                .filter(diagnostic -> diagnostic.code().equals("EANIM-FORGE-BODY-CLIP"))
                .findFirst()
                .orElseThrow()
                .message();
        Assertions.assertTrue(message.contains("wave.animation-clip.es"), message);
        Assertions.assertTrue(message.contains("Строка:"), message);
        Assertions.assertTrue(message.contains("Неизвестный bone track: right_hand"), message);
        Assertions.assertTrue(state.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("EANIM-FORGE-CATALOG-RETAINED")));
    }

    @Test
    void highLevelAnimationRequiresValidLifecycleClipsAndIsInstalled() {
        AnimationResourceCandidateCompiler candidates =
                new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler());

        AnimationCatalogCandidate candidate = candidates.compile(List.of(rig(),
                staticClip("wave_enter.animation-clip.es", "jdh:wave_enter", "right_forearm"),
                staticClip("wave_loop.animation-clip.es", "jdh:wave_loop", "right_forearm"),
                staticClip("wave_exit.animation-clip.es", "jdh:wave_exit", "right_forearm"),
                waveAnimation()));

        Assertions.assertTrue(candidate.valid(), candidate.diagnostics().toString());
        Assertions.assertEquals(List.of(AnimationId.parse("jdh:wave")),
                candidate.resourceSet().animations().stream().map(animation -> animation.id()).toList());
    }

    @Test
    void directLoopWithoutEnterOrExitIsInstalled() {
        AnimationResourceCandidateCompiler candidates =
                new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler());

        AnimationCatalogCandidate candidate = candidates.compile(List.of(rig(),
                staticClip("walk_main.animation-clip.es", "jdh:walk_main", "right_forearm"),
                directAnimation("walk.animation.es", "jdh:walk", "jdh:walk_main", "LOOP_UNTIL_STOP")));

        Assertions.assertTrue(candidate.valid(), candidate.diagnostics().toString());
        var animation = candidate.resourceSet().animations().stream()
                .filter(item -> item.id().equals(AnimationId.parse("jdh:walk"))).findFirst().orElseThrow();
        Assertions.assertTrue(animation.entryVariants().isEmpty());
        Assertions.assertTrue(animation.exitVariants().isEmpty());
        Assertions.assertEquals("LOOP_UNTIL_STOP", animation.main().mode().name());
    }

    @Test
    void directHoldWithoutHugeDurationOrExitIsInstalled() {
        AnimationResourceCandidateCompiler candidates =
                new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler());

        AnimationCatalogCandidate candidate = candidates.compile(List.of(rig(),
                staticClip("pose_main.animation-clip.es", "jdh:pose_main", "right_forearm"),
                directAnimation("pose.animation.es", "jdh:pose", "jdh:pose_main", "HOLD_UNTIL_STOP")));

        Assertions.assertTrue(candidate.valid(), candidate.diagnostics().toString());
        var animation = candidate.resourceSet().animations().stream()
                .filter(item -> item.id().equals(AnimationId.parse("jdh:pose"))).findFirst().orElseThrow();
        Assertions.assertEquals(12L, animation.main().durationTicks());
        Assertions.assertEquals("HOLD_UNTIL_STOP", animation.main().mode().name());
        Assertions.assertTrue(animation.entryVariants().isEmpty());
        Assertions.assertTrue(animation.exitVariants().isEmpty());
    }

    @Test
    void animationProfileRequiresExistingRigAndIsInstalled() {
        AnimationResourceCandidateCompiler candidates =
                new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler());

        AnimationCatalogCandidate candidate = candidates.compile(List.of(rig(), profile()));

        Assertions.assertTrue(candidate.valid(), candidate.diagnostics().toString());
        Assertions.assertEquals(List.of(AnimationProfileId.parse("jdh:standard_humanoid")),
                candidate.resourceSet().animationProfiles().stream().map(profile -> profile.id()).toList());
    }

    private AnimationResourceText rig() {
        return new AnimationResourceText("humanoid.animation-rig.es",
                TestSources.read("/ee1/fixtures/AnimationResourceCandidateCompilerTest-1.es"));
    }

    private AnimationResourceText clip(String sourceId, String boneId, int forearmRoll) {
        return new AnimationResourceText(sourceId,
                TestSources.read("/ee1/fixtures/AnimationResourceCandidateCompilerTest-2.es")
                        .formatted(sourceId.replace(".animation-clip.es", ""), boneId, forearmRoll));
    }

    private AnimationResourceText staticClip(String sourceId, String clipId, String boneId) {
        return new AnimationResourceText(sourceId,
                TestSources.read("/ee1/fixtures/AnimationResourceCandidateCompilerTest-3.es").formatted(clipId,
                boneId));
    }

    private AnimationResourceText waveAnimation() {
        return new AnimationResourceText("wave.animation.es",
                TestSources.read("/ee1/fixtures/AnimationResourceCandidateCompilerTest-4.es"));
    }

    private AnimationResourceText directAnimation(
            String sourceId,
            String animationId,
            String clipId,
            String mode
    ) {
        return new AnimationResourceText(sourceId,
                TestSources.read("/ee1/fixtures/AnimationResourceCandidateCompilerTest-5.es").formatted(animationId,
                clipId, mode));
    }

    private AnimationResourceText profile() {
        return new AnimationResourceText("standard.animation-profile.es",
                TestSources.read("/ee1/fixtures/AnimationResourceCandidateCompilerTest-6.es"));
    }
}
