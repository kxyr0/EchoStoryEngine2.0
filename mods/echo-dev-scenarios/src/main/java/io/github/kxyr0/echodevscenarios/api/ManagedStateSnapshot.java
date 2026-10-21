package io.github.kxyr0.echodevscenarios.api;

import java.util.Objects;

public record ManagedStateSnapshot(String token) {
    public ManagedStateSnapshot {
        token = Objects.requireNonNull(token, "token");
        if (token.isBlank()) throw new IllegalArgumentException("Managed snapshot token must not be blank.");
    }
}
