package io.github.kxyr0.echoitem.api.definition;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.ItemKindId;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoitem.api.text.ItemTextDefinition;
import io.github.kxyr0.echoitem.api.visual.ItemVisualDefinition;

/**
 * Immutable typed item definition shared by all item kinds.
 */
public interface ItemDefinition {
    EchoItemId id();

    ItemKindId kind();

    ItemTextDefinition text();

    ItemVisualDefinition visual();

    ItemDefinitionRevision revision();

    ItemSourceFingerprint sourceFingerprint();

    boolean available();
}
