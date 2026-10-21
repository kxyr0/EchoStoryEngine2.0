package io.github.kxyr0.echotrigger.core.definition;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TriggerDefinitionCatalog {
    private final Map<TriggerId, CompiledTriggerDefinition> definitions;

    public TriggerDefinitionCatalog(List<CompiledTriggerDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        Map<TriggerId, CompiledTriggerDefinition> byId = new LinkedHashMap<>();
        definitions.forEach(definition -> byId.put(definition.id(), definition));
        this.definitions = Map.copyOf(byId);
    }

    public Optional<CompiledTriggerDefinition> find(TriggerId id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public List<CompiledTriggerDefinition> definitions() {
        return List.copyOf(definitions.values());
    }

    public TriggerDefinitionCatalog runtimeView() {
        return new TriggerDefinitionCatalog(definitions.values().stream()
                .filter(value -> value.eventSubscription().isEmpty()).toList());
    }

    public List<TriggerStorySubscription> storySubscriptions() {
        return definitions.values().stream()
                .flatMap(definition -> definition.storySubscription().stream())
                .toList();
    }

    public List<TriggerStorySubscription> storySubscriptions(TriggerEventSelector selector) {
        return storySubscriptions().stream()
                .filter(subscription -> subscription.selector().equals(selector))
                .toList();
    }
}
