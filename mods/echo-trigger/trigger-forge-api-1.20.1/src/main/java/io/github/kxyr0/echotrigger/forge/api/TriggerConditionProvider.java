package io.github.kxyr0.echotrigger.forge.api;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.model.TriggerCondition;

public interface TriggerConditionProvider {
    boolean matches(TriggerCondition condition, TriggerEvent event);
}
