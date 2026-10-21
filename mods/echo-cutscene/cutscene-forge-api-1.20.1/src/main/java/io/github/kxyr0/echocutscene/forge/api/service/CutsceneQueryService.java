package io.github.kxyr0.echocutscene.forge.api.service;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public interface CutsceneQueryService {
    Optional<CutscenePlaybackView> activePlayback(ServerPlayer player);
}
