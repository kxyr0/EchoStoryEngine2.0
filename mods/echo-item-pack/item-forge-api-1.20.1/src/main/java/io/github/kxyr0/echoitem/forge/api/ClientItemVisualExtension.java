package io.github.kxyr0.echoitem.forge.api;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.visual.ItemVisualDefinition;

/**
 * Optional client visual hook for future item kinds.
 */
public interface ClientItemVisualExtension {
    boolean supports(EchoItemId id);

    ItemVisualDefinition visual(EchoItemId id);
}
