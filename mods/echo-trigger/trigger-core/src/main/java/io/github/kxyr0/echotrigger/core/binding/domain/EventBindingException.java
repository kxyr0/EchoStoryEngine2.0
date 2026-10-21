package io.github.kxyr0.echotrigger.core.binding.domain;

import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition.Location;

public final class EventBindingException extends IllegalArgumentException {
    private final Location source;
    EventBindingException(String message, Location source) {
        super(message);
        this.source = source;
    }
    public Location source() { return source; }
}
