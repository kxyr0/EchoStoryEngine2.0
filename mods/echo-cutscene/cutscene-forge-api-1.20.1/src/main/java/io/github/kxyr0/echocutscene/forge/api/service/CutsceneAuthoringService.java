package io.github.kxyr0.echocutscene.forge.api.service;

import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import net.minecraft.server.level.ServerPlayer;

public interface CutsceneAuthoringService {
    default CutsceneResult enter(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId) {
        return enter(player, cutsceneId, commandId, 0.28D);
    }

    CutsceneResult enter(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId, double cameraSpeed);

    CutsceneResult exit(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId);

    boolean isAuthoring(ServerPlayer player);

    CutsceneResult setMode(ServerPlayer player, CameraControlMode mode, CutsceneCommandId commandId);

    CutsceneResult setTarget(ServerPlayer player, CameraTarget target, CutsceneCommandId commandId);

    CutsceneResult clearTarget(ServerPlayer player, CutsceneCommandId commandId);
}
