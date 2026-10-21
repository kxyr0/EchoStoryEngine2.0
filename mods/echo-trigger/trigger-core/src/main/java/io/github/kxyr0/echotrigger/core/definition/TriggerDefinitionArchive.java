package io.github.kxyr0.echotrigger.core.definition;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class TriggerDefinitionArchive {
    private final Map<TriggerId, CompiledTriggerDefinition> knownGood = new LinkedHashMap<>();

    public void remember(CompiledTriggerDefinition definition) {
        knownGood.put(definition.id(), definition);
    }

    public Optional<CompiledTriggerDefinition> knownGood(TriggerId id) {
        return Optional.ofNullable(knownGood.get(id));
    }
}
