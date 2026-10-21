package io.github.kxyr0.echoanimation.forge.client.reload;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AnimationClientHotReloadWatcherTest {
    @TempDir
    Path tempDir;

    @Test
    void fingerprintChangesWhenSupportedFileIsDeleted() throws Exception {
        Path file = tempDir.resolve("jdh")
                .resolve("echo_animation")
                .resolve("animation_clips")
                .resolve("humanoid_test_wave.animation-clip.es");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "clip", StandardCharsets.UTF_8);
        long withClip = AnimationReloadFileSet.fingerprint(tempDir);

        Files.delete(file);
        long withoutClip = AnimationReloadFileSet.fingerprint(tempDir);
        Files.writeString(tempDir.resolve("ignored.txt"), "ignored", StandardCharsets.UTF_8);

        Assertions.assertNotEquals(withClip, withoutClip);
        Assertions.assertEquals(withoutClip, AnimationReloadFileSet.fingerprint(tempDir));
    }

    @Test
    void fingerprintChangesWhenHighLevelAnimationIsCreated() throws Exception {
        Path file = tempDir.resolve("jdh")
                .resolve("echo_animation")
                .resolve("animations")
                .resolve("wave.animation.es");
        Files.createDirectories(file.getParent());
        long before = AnimationReloadFileSet.fingerprint(tempDir);

        Files.writeString(file, "animation", StandardCharsets.UTF_8);

        Assertions.assertNotEquals(before, AnimationReloadFileSet.fingerprint(tempDir));
    }

    @Test
    void rapidChangesAreCoalescedAndScheduledOnClientExecutor() throws Exception {
        AtomicInteger scheduled = new AtomicInteger();
        AtomicInteger reloads = new AtomicInteger();
        CountDownLatch reloaded = new CountDownLatch(1);
        AnimationClientReloadScheduler scheduler = scheduler(scheduled, reloads, reloaded);

        scheduler.markChanged();
        scheduler.markChanged();
        Thread.sleep(100L);
        scheduler.reloadIfDebounced(Duration.ofMillis(80L));
        scheduler.reloadIfDebounced(Duration.ofMillis(80L));

        Assertions.assertTrue(reloaded.await(1L, TimeUnit.SECONDS));
        Assertions.assertEquals(1, scheduled.get());
        Assertions.assertEquals(1, reloads.get());
    }

    @Test
    void watchesContentPackCreatedAfterStartup() throws Exception {
        Path root = tempDir.resolve("story").resolve("content-pack");
        AtomicInteger scheduled = new AtomicInteger();
        AtomicInteger reloads = new AtomicInteger();
        CountDownLatch reloaded = new CountDownLatch(1);

        try (AnimationClientHotReloadWatcher watcher = watcher(root, scheduled, reloads, reloaded,
                Duration.ofSeconds(30L))) {
            watcher.start();
            Thread.sleep(80L);
            Files.createDirectories(root);
            Thread.sleep(200L);
            Path directory = root.resolve("jdh").resolve("echo_animation").resolve("animations");
            Files.createDirectories(directory);
            Thread.sleep(200L);
            Files.writeString(directory.resolve("wave.animation.es"), "animation", StandardCharsets.UTF_8);

            Assertions.assertTrue(reloaded.await(3L, TimeUnit.SECONDS));
        }

        Assertions.assertEquals(1, scheduled.get());
        Assertions.assertEquals(1, reloads.get());
    }

    @Test
    void periodicReconciliationSchedulesReloadForMissedExternalFileCreation() throws Exception {
        Path root = tempDir.resolve("content-pack");
        AtomicInteger scheduled = new AtomicInteger();
        AtomicInteger reloads = new AtomicInteger();
        CountDownLatch reloaded = new CountDownLatch(1);

        try (AnimationClientHotReloadWatcher watcher = watcher(root, scheduled, reloads, reloaded,
                Duration.ofMillis(40L))) {
            watcher.start();
            Thread.sleep(120L);
            Path file = root.resolve("jdh")
                    .resolve("echo_animation")
                    .resolve("animation_rigs")
                    .resolve("humanoid.animation-rig.es");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "rig", StandardCharsets.UTF_8);

            Assertions.assertTrue(reloaded.await(3L, TimeUnit.SECONDS));
        }

        Assertions.assertEquals(1, scheduled.get());
        Assertions.assertEquals(1, reloads.get());
    }

    private AnimationClientHotReloadWatcher watcher(Path root, AtomicInteger scheduled, AtomicInteger reloads,
            CountDownLatch reloaded, Duration reconcile) {
        return new AnimationClientHotReloadWatcher(root, scheduler(scheduled, reloads, reloaded),
                new AnimationClientHotReloadWatcher.Timing(Duration.ofMillis(20L), Duration.ofMillis(80L),
                        reconcile));
    }

    private AnimationClientReloadScheduler scheduler(AtomicInteger scheduled, AtomicInteger reloads,
            CountDownLatch reloaded) {
        return new AnimationClientReloadScheduler(task -> {
            scheduled.incrementAndGet();
            task.run();
        }, () -> {
            reloads.incrementAndGet();
            reloaded.countDown();
        });
    }
}
