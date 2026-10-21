package io.github.kxyr0.echomagic.forge.api.service;

import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;

public interface MagicProjectileService {
    void projectileTerminal(SpellProjectileId projectileId);
    int activeProjectileCount();
}

