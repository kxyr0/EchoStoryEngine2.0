package io.github.kxyr0.echoeffects.forge.api;

import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import java.util.List;
import java.util.Optional;

public interface EffectDefinitionCatalog {
    Optional<EffectDefinition> find(EffectId id);

    List<EffectDefinition> definitions();
}
