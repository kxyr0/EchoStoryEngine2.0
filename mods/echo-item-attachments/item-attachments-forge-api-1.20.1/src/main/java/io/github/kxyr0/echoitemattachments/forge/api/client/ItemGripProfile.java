package io.github.kxyr0.echoitemattachments.forge.api.client;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import java.util.Map;
import java.util.Optional;

public record ItemGripProfile(Optional<String> itemId,
        Map<AttachmentPointId, ItemGripTransform> transforms) {
    public ItemGripProfile {
        itemId = itemId.map(String::trim);
        transforms = Map.copyOf(transforms);
    }
}
