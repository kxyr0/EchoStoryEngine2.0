package io.github.kxyr0.echomagic.forge.api;

import io.github.kxyr0.echomagic.forge.api.service.KnownSpellService;
import io.github.kxyr0.echomagic.forge.api.service.MagicProjectileService;
import io.github.kxyr0.echomagic.forge.api.service.SpellCastService;
import io.github.kxyr0.echomagic.forge.api.service.SpellSelectionService;

public interface MagicService extends MagicQueryService, SpellCatalog, KnownSpellService,
        SpellSelectionService, SpellCastService, MagicProjectileService { }

