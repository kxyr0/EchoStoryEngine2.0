package io.github.kxyr0.echocutscene.forge.service;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.forge.api.service.CutscenePlaybackView;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import io.github.kxyr0.echocutscene.forge.playback.ServerCutscenePlaybackHost;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeCutsceneService implements CutsceneService {
    private final ServerCutscenePlaybackHost host;

    public ForgeCutsceneService(ServerCutscenePlaybackHost host) {
        this.host = host;
    }

    @Override
    public CutsceneResult start(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId) {
        return host.start(player, cutsceneId, commandId);
    }

    @Override
    public CutsceneResult start(
            ServerPlayer player,
            CutsceneId cutsceneId,
            CutsceneCommandId commandId,
            CutsceneEndBehavior endBehavior
    ) {
        return host.start(player, cutsceneId, commandId, endBehavior);
    }

    @Override
    public CutsceneResult stop(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId) {
        return host.stop(player, cutsceneId, commandId);
    }

    @Override
    public boolean isPlaying(ServerPlayer player, CutsceneId cutsceneId) {
        return host.view(player).filter(view -> view.cutsceneId().equals(cutsceneId)).isPresent();
    }

    @Override
    public boolean isPrepared(ServerPlayer player, CutsceneId cutsceneId) {
        return host.view(player)
                .filter(view -> view.cutsceneId().equals(cutsceneId))
                .map(CutscenePlaybackView::prepared)
                .orElse(false);
    }

    @Override
    public void setBlack(ServerPlayer player, double canvasGroup, long timeMillis) {
        host.setBlack(player, canvasGroup, timeMillis);
    }
}
