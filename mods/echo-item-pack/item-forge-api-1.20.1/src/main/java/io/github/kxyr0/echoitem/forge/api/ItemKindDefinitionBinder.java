package io.github.kxyr0.echoitem.forge.api;

import io.github.kxyr0.echoitem.api.ItemKindId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinition;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionResult;

/**
 * Public semantic binder boundary for future item kinds.
 */
public interface ItemKindDefinitionBinder {
    ItemKindId kind();

    ItemDefinitionResult<? extends ItemDefinition> bind(String sourceId, String sourceText, long revision);
}
