package io.github.kxyr0.echotrigger.forge.api;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import java.util.List;

public interface TriggerEventProvider {
    List<TriggerEvent> pollEvents(long serverTick);
}
