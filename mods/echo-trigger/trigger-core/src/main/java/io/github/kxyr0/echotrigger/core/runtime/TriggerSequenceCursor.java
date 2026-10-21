package io.github.kxyr0.echotrigger.core.runtime;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.model.TriggerCondition;

final class TriggerSequenceCursor {
    private final TriggerEventMatcher matcher = new TriggerEventMatcher();

    TriggerRuntimeInstance apply(TriggerRuntimeInstance instance, TriggerEvent event) {
        if (instance.status().isTerminal() || instance.step() >= instance.definition().sequence().size()) {
            return instance;
        }
        if (!instance.owner().equals(event.owner())) {
            return instance;
        }
        TriggerCondition condition = instance.definition().sequence().get(instance.step()).expression().condition();
        if (!matcher.matches(condition, event)) {
            return instance;
        }
        int nextStep = instance.step() + 1;
        if (nextStep >= instance.definition().sequence().size()) {
            return instance.completed();
        }
        return instance.with(nextStep, instance.status(), instance.outcome());
    }
}
