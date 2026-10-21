package io.github.kxyr0.echostoryengine.story.forge.persistence;

import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Single-worker bounded persistence queue owned by one server host.
 */
public final class BoundedPersistenceWorkQueue implements PersistenceWorkQueue {
    private final int capacity;
    private final ExecutorService worker;
    private final Queue<Runnable> completions = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pending = new AtomicInteger();
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final Consumer<RuntimeException> failureSink;

    public BoundedPersistenceWorkQueue(
            int capacity,
            String threadName,
            Consumer<RuntimeException> failureSink) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.failureSink = Objects.requireNonNull(failureSink, "failureSink");
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, Objects.requireNonNull(threadName, "threadName"));
            thread.setDaemon(true);
            return thread;
        };
        this.worker = Executors.newSingleThreadExecutor(threadFactory);
    }

    @Override
    public boolean submit(PersistenceWork work) {
        Objects.requireNonNull(work, "work");
        if (!reserveSlot()) {
            return false;
        }
        try {
            worker.execute(() -> runWork(work));
            return true;
        } catch (RejectedExecutionException exception) {
            pending.decrementAndGet();
            return false;
        }
    }

    @Override
    public int drain(int maxResults) {
        int drained = 0;
        while (drained < maxResults) {
            Runnable completion = completions.poll();
            if (completion == null) {
                return drained;
            }
            completion.run();
            pending.decrementAndGet();
            drained++;
        }
        return drained;
    }

    @Override
    public void shutdown(Duration timeout) {
        accepting.set(false);
        worker.shutdown();
        try {
            worker.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public int pendingCount() {
        return pending.get();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    private boolean reserveSlot() {
        while (accepting.get()) {
            int current = pending.get();
            if (current >= capacity) {
                return false;
            }
            if (pending.compareAndSet(current, current + 1)) {
                return true;
            }
        }
        return false;
    }

    private void runWork(PersistenceWork work) {
        try {
            completions.add(work.run());
        } catch (RuntimeException exception) {
            completions.add(() -> failureSink.accept(exception));
        }
    }
}
