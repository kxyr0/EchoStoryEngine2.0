package io.github.kxyr0.echotrigger.core.persistence;

import java.util.List;
import java.util.Objects;

public record TriggerPersistenceImage(int schemaVersion, List<TriggerInstanceState> instances) {
    public TriggerPersistenceImage {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive.");
        }
        instances = List.copyOf(Objects.requireNonNull(instances, "instances"));
    }
}
