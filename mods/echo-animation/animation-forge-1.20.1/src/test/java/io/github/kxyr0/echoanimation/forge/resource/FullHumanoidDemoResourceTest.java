package io.github.kxyr0.echoanimation.forge.resource;

import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModelReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class FullHumanoidDemoResourceTest {
    private static final List<String> SCRIPTS = List.of(
            "data/echo_animation/echo_animation/animation_rigs/tests/full_humanoid.animation-rig.es",
            "data/echo_animation/echo_animation/animation_clips/tests/full_humanoid.animation-clip.es",
            "data/echo_animation/echo_animation/animation_clips/tests/full_humanoid_matrix.animation-clip.es");

    @Test
    void fullHumanoidRigAndClipCompile() {
        List<AnimationResourceText> resources = SCRIPTS.stream()
                .map(path -> new AnimationResourceText(path, text(path))).toList();
        AnimationCatalogCandidate candidate = new AnimationResourceCandidateCompiler(
                new EchoScriptAnimationCompiler()).compile(resources);

        Assertions.assertTrue(candidate.valid(), candidate.diagnostics().toString());
        Assertions.assertEquals(1, candidate.resourceSet().animationRigs().size());
        Assertions.assertEquals(2, candidate.resourceSet().animationClips().size());
        var legacy = candidate.resourceSet().animationClips().stream()
                .filter(clip -> clip.id().value().equals("echo_animation:full_humanoid_test"))
                .findFirst().orElseThrow();
        var matrix = candidate.resourceSet().animationClips().stream()
                .filter(clip -> clip.id().value().equals("echo_animation:full_humanoid_matrix_test"))
                .findFirst().orElseThrow();
        Assertions.assertEquals(11, legacy.tracks().size());
        Assertions.assertEquals(1, matrix.tracks().size());
        Assertions.assertTrue(matrix.tracks().get(0).matrixTrack());
    }

    @Test
    void classicAndSlimModelsContainFourSoftJointsAndCorrectArmWidths() {
        var classic = model("full_humanoid");
        var slim = model("full_humanoid_slim");

        assertModel(classic);
        assertModel(slim);
        Assertions.assertEquals(8.25F, maximumX(classic), 0.0001F, "classic arm width");
        Assertions.assertEquals(7.25F, maximumX(slim), 0.0001F, "slim arm width");
        Assertions.assertTrue(slim.meshes().get(0).vertices().stream()
                .anyMatch(vertex -> Math.abs(vertex.u() - 55.0F / 64.0F) < 0.0001F),
                "slim left sleeve must end at the three-pixel UV edge");
        Assertions.assertEquals(4.0F / 64.0F, frontLegMinimumU(classic, true), 0.0001F,
                "positive-X leg must use the right-leg atlas half after entity mirroring");
        Assertions.assertEquals(20.0F / 64.0F, frontLegMinimumU(classic, false), 0.0001F,
                "negative-X leg must use the left-leg atlas half after entity mirroring");
    }

    private io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel model(String name) {
        String path = "assets/echo_animation/echo_animation/models/tests/" + name + ".skin.json";
        return new SkinnedModelReader().read(path, reader(path), 48);
    }

    private void assertModel(io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel model) {
        long halfWeighted = model.meshes().get(0).vertices().stream()
                .filter(vertex -> vertex.skin().weight(0) == 0.5F && vertex.skin().weight(1) == 0.5F)
                .count();
        Assertions.assertEquals(13, model.bones().size());
        Assertions.assertEquals(672, model.vertexCount());
        Assertions.assertTrue(halfWeighted >= 32, "elbows and knees must contain half-weight rows");
        assertRawTriangles(model.meshes().get(0));
    }

    private float maximumX(io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel model) {
        return model.meshes().get(0).vertices().stream()
                .map(vertex -> vertex.x()).max(Float::compare).orElseThrow();
    }

    private float frontLegMinimumU(
            io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel model,
            boolean positiveX
    ) {
        return model.meshes().get(0).vertices().stream()
                .filter(vertex -> vertex.y() >= 12.0F && vertex.z() == -2.0F && vertex.normalZ() == -1.0F)
                .filter(vertex -> usesLegBone(vertex, positiveX))
                .map(vertex -> vertex.u()).min(Float::compare).orElseThrow();
    }

    private boolean usesLegBone(
            io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedVertex vertex,
            boolean positiveX
    ) {
        int first = positiveX ? 9 : 11;
        for (int slot = 0; slot < 4; slot++) {
            if (vertex.skin().weight(slot) > 0.0F
                    && (vertex.skin().index(slot) == first || vertex.skin().index(slot) == first + 1)) {
                return true;
            }
        }
        return false;
    }

    private void assertRawTriangles(io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedMesh mesh) {
        int[] triangles = mesh.triangles();
        Assertions.assertEquals(0, triangles.length % 3, "triangle index count");
        for (int index : triangles) {
            Assertions.assertTrue(index >= 0 && index < mesh.vertices().size(), "triangle index " + index);
        }
        for (int offset = 0; offset < triangles.length; offset += 3) {
            var a = mesh.vertices().get(triangles[offset]);
            var b = mesh.vertices().get(triangles[offset + 1]);
            var c = mesh.vertices().get(triangles[offset + 2]);
            float abX = b.x() - a.x();
            float abY = b.y() - a.y();
            float abZ = b.z() - a.z();
            float acX = c.x() - a.x();
            float acY = c.y() - a.y();
            float acZ = c.z() - a.z();
            float crossX = abY * acZ - abZ * acY;
            float crossY = abZ * acX - abX * acZ;
            float crossZ = abX * acY - abY * acX;
            float areaSquared = crossX * crossX + crossY * crossY + crossZ * crossZ;
            Assertions.assertTrue(areaSquared > 0.000001F, "degenerate triangle at " + offset / 3);
        }
    }

    private String text(String path) {
        try (var input = reader(path)) {
            char[] buffer = new char[4096];
            StringBuilder result = new StringBuilder();
            for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
                result.append(buffer, 0, read);
            }
            return result.toString();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private InputStreamReader reader(String path) {
        var stream = FullHumanoidDemoResourceTest.class.getClassLoader().getResourceAsStream(path);
        return new InputStreamReader(java.util.Objects.requireNonNull(stream, path), StandardCharsets.UTF_8);
    }
}
