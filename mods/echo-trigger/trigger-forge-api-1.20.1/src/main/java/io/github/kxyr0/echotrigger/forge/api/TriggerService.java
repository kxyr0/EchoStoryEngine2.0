package io.github.kxyr0.echotrigger.forge.api;

import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.model.TriggerResult;

public interface TriggerService {
    TriggerResult start(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId);

    TriggerResult stop(TriggerInstanceId instanceId, TriggerCommandId commandId);

    TriggerResult reset(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId);

    TriggerResult fire(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId);
}
