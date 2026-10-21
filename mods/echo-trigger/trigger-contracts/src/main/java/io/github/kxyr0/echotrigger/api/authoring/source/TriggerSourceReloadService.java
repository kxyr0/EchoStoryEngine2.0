package io.github.kxyr0.echotrigger.api.authoring.source;

import io.github.kxyr0.echotrigger.api.id.TriggerId;

public interface TriggerSourceReloadService {
    TriggerSourceReloadResult reload(TriggerId expectedId);
}
