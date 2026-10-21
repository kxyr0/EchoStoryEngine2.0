package io.github.kxyr0.echotrigger.forge.api;

import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.model.TriggerSnapshot;
import java.util.Optional;

public interface TriggerQueryService {
    Optional<TriggerSnapshot> query(TriggerInstanceId instanceId);
}
