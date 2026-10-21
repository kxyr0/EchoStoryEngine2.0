package io.github.kxyr0.echoplayersleep.forge.network.event;

import io.github.kxyr0.echoplayersleep.forge.network.message.EyeTransitionMessage;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class ClientboundEyeTransitionEvent extends Event {
    private final EyeTransitionMessage message;

    public ClientboundEyeTransitionEvent(EyeTransitionMessage message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    public EyeTransitionMessage message() {
        return message;
    }
}
