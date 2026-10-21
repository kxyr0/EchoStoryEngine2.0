package io.github.kxyr0.echoanimation.forge.resource.migration;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.playback.NormalizedClipSampler;
import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceCandidateCompiler;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceText;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class HumanoidRunMigrationTest {
    private final NormalizedClipSampler sampler = new NormalizedClipSampler();

    @Test
    void migratedResourcesCompileAsOneClipAndPreserveAllLegacyPhasePoses() throws IOException {
        Path resources = contentResources();
        List<AnimationResourceText> sources = List.of(
                source(resources, "data/jdh/echo_animation/animation_rigs/chapter_1/family/humanoid.animation-rig.es"),
                source(resources, "data/jdh/echo_animation/animation_clips/chapter_1/family/run/"
                        + "humanoid_run.animation-clip.es"),
                source(resources, "data/jdh/echo_animation/animations/chapter_1/family/humanoid_run.animation.es"));
        var candidate = new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler()).compile(sources);

        Assertions.assertTrue(candidate.valid(), candidate.diagnostics().toString());
        AnimationClipDefinition migrated = candidate.resourceSet().animationClips().get(0);
        Assertions.assertEquals(17, migrated.durationTicks());
        comparePhase(migrated, "idle_to_run", LegacyRunClipFixtures.enter());
        compareLoop(migrated, LegacyRunClipFixtures.loop());
        comparePhase(migrated, "run_to_idle", LegacyRunClipFixtures.exit());
    }

    private void compareLoop(AnimationClipDefinition migrated, AnimationClipDefinition legacy) {
        for (double progress : List.of(0.0D, 0.25D, 0.5D, 0.75D, 1.0D)) {
            AnimationPose actual = sampler.sample(migrated, emptyPose(), progress,
                    Optional.of("run_loop"), Optional.empty());
            assertPose(sampler.sample(legacy, emptyPose(), progress), actual);
        }
    }

    private void comparePhase(AnimationClipDefinition migrated, String segment, AnimationClipDefinition legacy) {
        for (double progress : List.of(0.0D, 1.0D)) {
            AnimationPose actual = sampler.sample(migrated, emptyPose(), progress,
                    Optional.of(segment), Optional.empty());
            assertPose(sampler.sample(legacy, emptyPose(), progress), actual);
        }
    }

    private void assertPose(AnimationPose expected, AnimationPose actual) {
        Assertions.assertEquals(expected.transforms().keySet(), actual.transforms().keySet());
        for (var bone : expected.transforms().keySet()) {
            EulerTransform left = expected.transform(bone).orElseThrow();
            EulerTransform right = actual.transform(bone).orElseThrow();
            assertVector(left.translation(), right.translation(), bone + " translation");
            assertVector(left.rotationDegrees(), right.rotationDegrees(), bone + " rotation");
            assertVector(left.scale(), right.scale(), bone + " scale");
        }
    }

    private void assertVector(io.github.kxyr0.echoanimation.api.math.Vec3 expected,
            io.github.kxyr0.echoanimation.api.math.Vec3 actual, String label) {
        Assertions.assertEquals(expected.x(), actual.x(), 0.0001D, label + " x");
        Assertions.assertEquals(expected.y(), actual.y(), 0.0001D, label + " y");
        Assertions.assertEquals(expected.z(), actual.z(), 0.0001D, label + " z");
    }

    private AnimationPose emptyPose() {
        return new AnimationPose(Map.of());
    }

    private AnimationResourceText source(Path root, String relative) throws IOException {
        Path path = root.resolve(relative.replace('/', java.io.File.separatorChar));
        return new AnimationResourceText(relative, Files.readString(path));
    }

    private Path contentResources() {
        Path working = Path.of("").toAbsolutePath();
        List<Path> candidates = List.of(
                working.resolve("../echo-story-content-pack/src/main/resources").normalize(),
                working.resolve("../../echo-story-content-pack/src/main/resources").normalize(),
                working.resolve("mods/echo-story-content-pack/src/main/resources").normalize());
        return candidates.stream().filter(Files::isDirectory).findFirst()
                .orElseThrow(() -> new IllegalStateException("echo-story-content-pack resources not found from "
                        + working));
    }
}
