package io.github.kxyr0.echodialogue.forge.persistence;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Host-owned bounded serial scheduler for blocking persistence I/O.
 */
public final class ForgePersistenceScheduler {
    public static final int DEFAULT_QUEUE_CAPACITY = 64;
    public static final int DEFAULT_RESULT_CAPACITY = 64;

    private final BlockingQueue<Runnable> operations;
    private final BlockingQueue<Runnable> completions;
    private final Thread worker;
    private volatile boolean stopping;
    private final AtomicInteger droppedCompletions = new AtomicInteger();

    public ForgePersistenceScheduler(String threadName) {
        this(DEFAULT_QUEUE_CAPACITY, DEFAULT_RESULT_CAPACITY, threadName);
    }

    public ForgePersistenceScheduler(int queueCapacity, int resultCapacity, String threadName) {
        this.operations = new ArrayBlockingQueue<>(queueCapacity);
        this.completions = new ArrayBlockingQueue<>(resultCapacity);
        this.worker = new Thread(this::runWorker, threadName);
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public <T> SubmitResult submit(Supplier<T> operation, Consumer<T> completion) {
        if (stopping) {
            return SubmitResult.SHUTDOWN;
        }
        boolean accepted = operations.offer(() -> complete(operation.get(), completion));
        return accepted ? SubmitResult.ACCEPTED : SubmitResult.QUEUE_SATURATED;
    }

    public int drainCompletions(int maxCompletions) {
        int drained = 0;
        Runnable completion = completions.poll();
        while (completion != null && drained < maxCompletions) {
            completion.run();
            drained++;
            completion = completions.poll();
        }
        return drained;
    }

    public int droppedCompletions() {
        return droppedCompletions.get();
    }

    public int queuedOperations() {
        return operations.size();
    }

    public int queuedCompletions() {
        return completions.size();
    }

    public boolean shutdown(Duration timeout) {
        stopping = true;
        try {
            worker.join(timeout.toMillis());
            return !worker.isAlive();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private <T> void complete(T result, Consumer<T> completion) {
        if (!completions.offer(() -> completion.accept(result))) {
            droppedCompletions.incrementAndGet();
        }
    }

    private void runWorker() {
        while (!stopping || !operations.isEmpty()) {
            try {
                Runnable operation = operations.poll(100, TimeUnit.MILLISECONDS);
                if (operation != null) {
                    runOperation(operation);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                stopping = true;
            }
        }
    }

    private void runOperation(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException exception) {
            droppedCompletions.incrementAndGet();
        }
    }

    public enum SubmitResult {
        ACCEPTED,
        QUEUE_SATURATED,
        SHUTDOWN
    }
}
