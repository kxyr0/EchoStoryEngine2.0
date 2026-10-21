package io.github.kxyr0.echocutscene.forge.api.body;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public interface PlayerBodyLockService {
    boolean locked(ServerPlayer player);

    Optional<CutscenePlaybackId> lockOwner(ServerPlayer player);
}
