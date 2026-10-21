package io.github.kxyr0.echoplayer.forge.api;

import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerAppearanceMutationService;
import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerDialogueMutationService;
import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerIdentityMutationService;
import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerSparkMutationService;
import io.github.kxyr0.echoplayer.forge.api.mutation.PlayerTemplateService;
import io.github.kxyr0.echoplayer.forge.api.query.PlayerProfileQueryService;

/**
 * Server-scoped public Echo Content Player service surface.
 */
public interface EchoPlayerService extends PlayerProfileQueryService, PlayerIdentityMutationService,
        PlayerSparkMutationService, PlayerAppearanceMutationService, PlayerTemplateService,
        PlayerDialogueMutationService {
}
