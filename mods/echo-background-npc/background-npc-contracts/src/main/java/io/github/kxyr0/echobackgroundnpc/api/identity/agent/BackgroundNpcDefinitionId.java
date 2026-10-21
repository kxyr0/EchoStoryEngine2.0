package io.github.kxyr0.echobackgroundnpc.api.identity.agent;

import java.util.Objects;

/** Stable resource definition identity, distinct from a runtime instance. */
public record BackgroundNpcDefinitionId(String value) {
    public BackgroundNpcDefinitionId {
        value = new BackgroundNpcId(Objects.requireNonNull(value, "value")).value();
    }
    @Override public String toString() { return value; }
}
