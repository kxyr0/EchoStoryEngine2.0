package io.github.kxyr0.echoplayer.forge.api.mutation;

import io.github.kxyr0.echoplayer.api.profile.PlayerProfileTemplateId;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;

/**
 * Public template reload and default-template query boundary.
 */
public interface PlayerTemplateService {
    PlayerProfileTemplateId defaultTemplateId();

    PlayerProfileResult<PlayerProfileTemplateId> reloadTemplates();
}
