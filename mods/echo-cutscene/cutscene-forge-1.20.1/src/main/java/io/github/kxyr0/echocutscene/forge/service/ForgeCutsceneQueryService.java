package io.github.kxyr0.echocutscene.forge.service;

import io.github.kxyr0.echocutscene.forge.api.service.CutscenePlaybackView;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneQueryService;
import io.github.kxyr0.echocutscene.forge.playback.ServerCutscenePlaybackHost;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeCutsceneQueryService implements CutsceneQueryService {
    private final ServerCutscenePlaybackHost host;

    public ForgeCutsceneQueryService(ServerCutscenePlaybackHost host) {
        this.host = host;
    }

    @Override
    public Optional<CutscenePlaybackView> activePlayback(ServerPlayer player) {
        return host.view(player);
    }
}
