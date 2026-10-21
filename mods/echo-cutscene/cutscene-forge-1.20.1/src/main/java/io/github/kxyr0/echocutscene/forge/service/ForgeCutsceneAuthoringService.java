package io.github.kxyr0.echocutscene.forge.service;

import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneAuthoringService;
import io.github.kxyr0.echocutscene.forge.authoring.ServerCutsceneAuthoringHost;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeCutsceneAuthoringService implements CutsceneAuthoringService {
    private final ServerCutsceneAuthoringHost host;

    public ForgeCutsceneAuthoringService(ServerCutsceneAuthoringHost host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    @Override
    public CutsceneResult enter(
            ServerPlayer player,
            CutsceneId cutsceneId,
            CutsceneCommandId commandId,
            double cameraSpeed
    ) {
        return host.enter(player, cutsceneId, commandId, cameraSpeed);
    }

    @Override
    public CutsceneResult exit(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId) {
        return host.exit(player, cutsceneId, commandId);
    }

    @Override
    public boolean isAuthoring(ServerPlayer player) {
        return host.activeCutscene(player).isPresent();
    }

    @Override
    public CutsceneResult setMode(ServerPlayer player, CameraControlMode mode, CutsceneCommandId commandId) {
        return host.setMode(player, mode, commandId);
    }

    @Override
    public CutsceneResult setTarget(ServerPlayer player, CameraTarget target, CutsceneCommandId commandId) {
        return host.setTarget(player, target, commandId);
    }

    @Override
    public CutsceneResult clearTarget(ServerPlayer player, CutsceneCommandId commandId) {
        return host.clearTarget(player, commandId);
    }
}
