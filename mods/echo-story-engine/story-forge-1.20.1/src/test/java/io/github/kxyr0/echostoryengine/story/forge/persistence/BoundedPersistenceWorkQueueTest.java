package io.github.kxyr0.echostoryengine.story.forge.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class BoundedPersistenceWorkQueueTest {
    @Test
    void queueRejectsWorkBeyondCapacityUntilCompletionIsDrained() throws InterruptedException {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();
        BoundedPersistenceWorkQueue queue = new BoundedPersistenceWorkQueue(
                1,
                "echo-story-engine-test",
                failure -> failures.incrementAndGet());

        assertTrue(queue.submit(() -> {
            entered.countDown();
            await(release);
            return () -> { };
        }));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        assertFalse(queue.submit(() -> () -> { }));

        release.countDown();
        queue.shutdown(Duration.ofSeconds(5));
        assertEquals(1, queue.drain(1));
        assertEquals(0, failures.get());
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
