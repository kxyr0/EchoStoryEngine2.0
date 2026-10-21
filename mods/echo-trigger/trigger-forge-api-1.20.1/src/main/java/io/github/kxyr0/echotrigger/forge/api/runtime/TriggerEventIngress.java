package io.github.kxyr0.echotrigger.forge.api.runtime;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.model.TriggerSnapshot;
import java.util.List;

public interface TriggerEventIngress {
    List<TriggerSnapshot> applyEvent(TriggerEvent event);
}
