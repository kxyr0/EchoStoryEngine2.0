package io.github.kxyr0.echocutscene.forge.api.client;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;

public interface DetachedCameraService {
    boolean detached();

    void detach(CameraPose initialPose);

    void attachToBody();
}
