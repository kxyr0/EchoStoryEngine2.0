package io.github.kxyr0.echoanimation.forge.client.reload;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class AnimationClientReloadScheduler {
    private final Consumer<Runnable> clientExecutor;
    private final Runnable reloadAction;
    private final AtomicBoolean reloadQueued = new AtomicBoolean();
    private long lastChangeMillis;

    AnimationClientReloadScheduler(Consumer<Runnable> clientExecutor, Runnable reloadAction) {
        this.clientExecutor = Objects.requireNonNull(clientExecutor, "clientExecutor");
        this.reloadAction = Objects.requireNonNull(reloadAction, "reloadAction");
    }

    void markChanged() {
        lastChangeMillis = System.currentTimeMillis();
    }

    void reloadIfDebounced(Duration debounce) {
        if (lastChangeMillis == 0L || System.currentTimeMillis() - lastChangeMillis < debounce.toMillis()) {
            return;
        }
        lastChangeMillis = 0L;
        if (!reloadQueued.compareAndSet(false, true)) {
            return;
        }
        clientExecutor.accept(() -> {
            try {
                reloadAction.run();
            } finally {
                reloadQueued.set(false);
            }
        });
    }
}
