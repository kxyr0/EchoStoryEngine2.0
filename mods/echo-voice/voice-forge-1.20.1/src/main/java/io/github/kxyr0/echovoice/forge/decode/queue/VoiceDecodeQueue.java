package io.github.kxyr0.echovoice.forge.decode.queue;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded prioritized decoder executor; no thread is created per file. */
public final class VoiceDecodeQueue implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final Semaphore capacity;
    private final AtomicLong sequence = new AtomicLong();

    public VoiceDecodeQueue(int workers, int queueCapacity) {
        capacity = new Semaphore(queueCapacity);
        executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>(), threadFactory());
        executor.prestartAllCoreThreads();
    }

    public <T> CompletableFuture<T> submit(VoiceDecodePriority priority, Callable<T> callable) {
        CompletableFuture<T> result = new CompletableFuture<>();
        if (!capacity.tryAcquire()) {
            result.completeExceptionally(new RejectedExecutionException("Voice decode queue is full."));
            return result;
        }
        PrioritizedTask<T> task = new PrioritizedTask<>(priority, sequence.incrementAndGet(), callable, result,
                capacity);
        try {
            executor.execute(task);
        } catch (RejectedExecutionException exception) {
            capacity.release();
            result.completeExceptionally(exception);
        }
        return result;
    }

    public int queuedTasks() {
        return executor.getQueue().size();
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private ThreadFactory threadFactory() {
        AtomicLong ids = new AtomicLong();
        return runnable -> {
            Thread thread = new Thread(runnable, "Echo Voice Decode " + ids.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static final class PrioritizedTask<T> extends FutureTask<T>
            implements Comparable<PrioritizedTask<?>> {
        private final VoiceDecodePriority priority;
        private final long sequence;
        private final CompletableFuture<T> result;
        private final Semaphore capacity;

        private PrioritizedTask(VoiceDecodePriority priority, long sequence, Callable<T> callable,
                CompletableFuture<T> result, Semaphore capacity) {
            super(callable);
            this.priority = priority;
            this.sequence = sequence;
            this.result = result;
            this.capacity = capacity;
        }

        @Override
        protected void done() {
            try {
                result.complete(get());
            } catch (Exception exception) {
                result.completeExceptionally(exception.getCause() == null ? exception : exception.getCause());
            } finally {
                capacity.release();
            }
        }

        @Override
        public int compareTo(PrioritizedTask<?> other) {
            int byPriority = Integer.compare(priority.order(), other.priority.order());
            return byPriority != 0 ? byPriority : Long.compare(sequence, other.sequence);
        }
    }
}
