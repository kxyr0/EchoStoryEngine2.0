package io.github.kxyr0.echoobservation.api.action;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ObservationActionRegistry {
    private final Map<String, ObservationActionExecutor> executors = new LinkedHashMap<>();

    public synchronized boolean register(ObservationActionExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        return executors.putIfAbsent(executor.target(), executor) == null;
    }

    public synchronized Optional<ObservationActionExecutor> resolve(String target) {
        return Optional.ofNullable(executors.get(target));
    }

    public synchronized void clear() {
        executors.clear();
    }
}
