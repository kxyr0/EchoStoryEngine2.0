package io.github.kxyr0.echocutscene.forge.api.service;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import net.minecraft.server.level.ServerPlayer;

public interface CutsceneService {
    CutsceneResult start(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId);

    default CutsceneResult start(
            ServerPlayer player,
            CutsceneId cutsceneId,
            CutsceneCommandId commandId,
            CutsceneEndBehavior endBehavior
    ) {
        return start(player, cutsceneId, commandId);
    }

    CutsceneResult stop(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId);

    boolean isPlaying(ServerPlayer player, CutsceneId cutsceneId);

    default boolean isPrepared(ServerPlayer player, CutsceneId cutsceneId) {
        return isPlaying(player, cutsceneId);
    }

    default void setBlack(ServerPlayer player, double canvasGroup, long timeMillis) {
    }
}
