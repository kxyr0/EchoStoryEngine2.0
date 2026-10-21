package io.github.kxyr0.echoitem.core.manifest;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.ItemKindId;
import java.util.Objects;

/**
 * Valid new ID discovered after registry freeze.
 */
public record PendingItemRegistration(EchoItemId id, ItemKindId kind, String message) {
    public PendingItemRegistration {
        id = Objects.requireNonNull(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        message = Objects.requireNonNull(message, "message");
    }
}
