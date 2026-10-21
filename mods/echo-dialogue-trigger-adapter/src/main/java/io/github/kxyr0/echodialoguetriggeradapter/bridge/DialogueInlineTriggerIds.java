package io.github.kxyr0.echodialoguetriggeradapter.bridge;

import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DialogueInlineTriggerIds {
    public TriggerInstanceId instanceId(DialogueTriggerSessionKey key) {
        return new TriggerInstanceId("dialogue-inline:" + UUID.nameUUIDFromBytes(
                key.stableSource().getBytes(StandardCharsets.UTF_8)));
    }
}
