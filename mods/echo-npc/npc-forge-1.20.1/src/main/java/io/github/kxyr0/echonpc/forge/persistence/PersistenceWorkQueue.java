package io.github.kxyr0.echonpc.forge.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Lifecycle-owned bounded serial I/O worker. Results are applied by the server thread.
 */
public final class PersistenceWorkQueue {
    public static final int CAPACITY = 64;
    private final ArrayBlockingQueue<Work> work;
    private final ConcurrentLinkedQueue<Completed> completed = new ConcurrentLinkedQueue<>();
    private final Thread worker;
    private volatile boolean accepting = true;

    public PersistenceWorkQueue(String threadName) {
        this.work = new ArrayBlockingQueue<>(CAPACITY);
        this.worker = new Thread(this::run, Objects.requireNonNull(threadName, "threadName"));
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public boolean submit(String operationId, Callable<Object> action) {
        if (!accepting) {
            return false;
        }
        return work.offer(new Work(operationId, action));
    }

    public List<Completed> drain(int limit) {
        int bounded = Math.max(0, Math.min(limit, CAPACITY));
        List<Completed> drained = new ArrayList<>();
        while (drained.size() < bounded) {
            Completed next = completed.poll();
            if (next == null) {
                break;
            }
            drained.add(next);
        }
        return drained;
    }

    public int queuedCount() {
        return work.size();
    }

    public boolean shutdown(long timeoutMillis) {
        accepting = false;
        try {
            worker.join(Math.max(1L, timeoutMillis));
            if (!worker.isAlive()) {
                return true;
            }
            worker.interrupt();
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void run() {
        while (accepting || !work.isEmpty()) {
            try {
                Work next = work.poll(100L, TimeUnit.MILLISECONDS);
                if (next != null) {
                    completed.add(next.execute());
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private record Work(String operationId, Callable<Object> action) {
        Work {
            operationId = Objects.requireNonNull(operationId, "operationId");
            action = Objects.requireNonNull(action, "action");
        }

        Completed execute() {
            try {
                return new Completed(operationId, action.call(), null);
            } catch (Exception exception) {
                return new Completed(operationId, null, exception);
            }
        }
    }

    public record Completed(String operationId, Object result, Exception failure) {
        public Completed {
            operationId = Objects.requireNonNull(operationId, "operationId");
        }

        public boolean failed() {
            return failure != null;
        }
    }
}
