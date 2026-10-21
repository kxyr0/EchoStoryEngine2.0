package io.github.kxyr0.echodialoguetriggeradapter.execution;

import io.github.kxyr0.echodialoguetriggeradapter.bridge.DialogueTriggerCommandIds;
import io.github.kxyr0.echodialoguetriggeradapter.bridge.DialogueTriggerSessionKey;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.model.TriggerResult;
import io.github.kxyr0.echotrigger.api.status.TriggerStatus;
import io.github.kxyr0.echotrigger.forge.api.TriggerQueryService;
import io.github.kxyr0.echotrigger.forge.api.TriggerService;

public final class DialogueTriggerCommandExecutor {
    private final DialogueTriggerCommandIds commandIds = new DialogueTriggerCommandIds();

    public TriggerResult start(
            TriggerService service,
            DialogueTriggerSessionKey key,
            TriggerId triggerId,
            TriggerOwnerId owner
    ) {
        return service.start(triggerId, owner, commandIds.resource(key, "start", triggerId));
    }

    public TriggerResult stop(TriggerService service, DialogueTriggerSessionKey key, TriggerInstanceId instanceId) {
        return service.stop(instanceId, commandIds.inline(key, "stop"));
    }

    public TriggerResult reset(
            TriggerService service,
            DialogueTriggerSessionKey key,
            TriggerId triggerId,
            TriggerOwnerId owner
    ) {
        return service.reset(triggerId, owner, commandIds.resource(key, "reset", triggerId));
    }

    public TriggerResult fire(
            TriggerService service,
            DialogueTriggerSessionKey key,
            TriggerId triggerId,
            TriggerOwnerId owner
    ) {
        return service.fire(triggerId, owner, commandIds.resource(key, "fire", triggerId));
    }

    public boolean completed(TriggerQueryService queryService, TriggerInstanceId instanceId) {
        return queryService.query(instanceId)
                .filter(snapshot -> snapshot.status() == TriggerStatus.COMPLETED)
                .isPresent();
    }
}
