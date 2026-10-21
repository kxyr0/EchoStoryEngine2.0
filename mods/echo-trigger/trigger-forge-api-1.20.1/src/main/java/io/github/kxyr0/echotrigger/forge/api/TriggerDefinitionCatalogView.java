package io.github.kxyr0.echotrigger.forge.api;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import java.util.List;
import java.util.Optional;

public interface TriggerDefinitionCatalogView {
    List<TriggerId> triggerIds();

    Optional<TriggerRevision> revision(TriggerId triggerId);
}
