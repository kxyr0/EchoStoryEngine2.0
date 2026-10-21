package io.github.kxyr0.echotrigger.core.runtime;

import io.github.kxyr0.echotrigger.api.id.TriggerExecutionId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class TriggerInstanceIds {
    private TriggerInstanceIds() {
    }

    public static TriggerInstanceId instance(TriggerId id, TriggerOwnerId owner, TriggerRevision revision) {
        return new TriggerInstanceId("trigger:" + stable(id + "|" + owner.value() + "|" + revision.value()));
    }

    public static TriggerInstanceId inline(String stableKey) {
        return new TriggerInstanceId("inline:" + stable(stableKey));
    }

    public static TriggerExecutionId execution(TriggerInstanceId instanceId, long generation) {
        return new TriggerExecutionId("exec:" + stable(instanceId.value() + "|" + generation));
    }

    private static String stable(String source) {
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
