package io.github.kxyr0.echowardrobe.forge.api.catalog;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import java.util.List;
import java.util.Optional;

/**
 * Query-only public catalog view.
 */
public interface WearableCatalogView {
    long catalogRevision();

    int wearableDefinitionCount();

    List<WearableDefinition> wearableDefinitions();

    Optional<WearableDefinition> wearable(WearableId id);
}
