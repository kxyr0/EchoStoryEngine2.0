package io.github.kxyr0.echoanimation.forge.resource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AnimationResourceDiscoveryTest {
    @TempDir
    Path tempDir;

    @Test
    void externalWorkspaceStartsAtNamespaceDirectory() throws Exception {
        Path valid = tempDir.resolve("jdh")
                .resolve("echo_animation")
                .resolve("animation_clips")
                .resolve("humanoid_test_wave.animation-clip.es");
        Files.createDirectories(valid.getParent());
        Files.writeString(valid, "clip", StandardCharsets.UTF_8);
        Path invalidParallelData = tempDir.resolve("data")
                .resolve("jdh")
                .resolve("echo_animation")
                .resolve("animation_clips")
                .resolve("wrong.animation-clip.es");
        Files.createDirectories(invalidParallelData.getParent());
        Files.writeString(invalidParallelData, "wrong", StandardCharsets.UTF_8);

        var resources = new AnimationResourceDiscovery().discoverExternal(tempDir);

        Assertions.assertEquals(1, resources.size());
        Assertions.assertEquals("jdh:echo_animation/animation_clips/humanoid_test_wave.animation-clip.es",
                resources.get(0).sourceId());
        Assertions.assertEquals("clip", resources.get(0).text());
    }
}
