package io.github.kxyr0.echoitem.forge.api;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.ItemKindId;

/**
 * Public marker for platform runtime factories without exposing host internals.
 */
public interface ItemKindRuntimeFactory<T> {
    ItemKindId kind();

    T create(EchoItemId id);
}
