package io.github.kxyr0.echotrigger.forge.api.callback;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackInvocation;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class TriggerCallbackInvocationEvent extends Event {
    private final TriggerCallbackInvocation invocation;

    public TriggerCallbackInvocationEvent(TriggerCallbackInvocation invocation) {
        this.invocation = Objects.requireNonNull(invocation, "invocation");
    }

    public TriggerCallbackInvocation invocation() {
        return invocation;
    }
}
