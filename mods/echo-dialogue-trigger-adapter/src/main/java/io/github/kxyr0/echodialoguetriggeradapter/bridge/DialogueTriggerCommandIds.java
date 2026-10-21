package io.github.kxyr0.echodialoguetriggeradapter.bridge;

import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DialogueTriggerCommandIds {
    public TriggerCommandId resource(DialogueTriggerSessionKey key, String operation, TriggerId triggerId) {
        return new TriggerCommandId(operation + ":" + stable(key.stableSource() + "|" + triggerId.value()));
    }

    public TriggerCommandId inline(DialogueTriggerSessionKey key, String operation) {
        return new TriggerCommandId(operation + ":" + stable(key.stableSource()));
    }

    private static String stable(String source) {
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
