package io.github.kxyr0.echocutscene.forge.client.authoring;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echocutscene.api.camera.framing.CameraFraming;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.core.target.CameraLookAtRotation;
import io.github.kxyr0.echocutscene.forge.client.target.ClientCameraTargetResolver;
import io.github.kxyr0.echocutscene.forge.client.target.ClientViewportAspect;
import io.github.kxyr0.echonavigation.api.position.Vector3;

public final class ClientAuthoringOrientation {
    private final ClientCameraTargetResolver targets = new ClientCameraTargetResolver();

    public CameraPose orient(CameraPose base, ClientAuthoringSemanticState semantic, double yaw, double pitch) {
        if (semantic.mode() == CameraControlMode.FREE) {
            return new CameraPose(base.position(), new CameraRotation(yaw, pitch, 0.0D), base.fieldOfView());
        }
        return semantic.target().flatMap(targets::resolve)
                .map(target -> lookAt(base, semantic, target))
                .orElse(base);
    }

    private static CameraPose lookAt(CameraPose base, ClientAuthoringSemanticState semantic, Vector3 target) {
        CameraFraming framing = semantic.mode() == CameraControlMode.LOOK_AT_WITH_OFFSET
                ? semantic.framing() : CameraFraming.CENTER;
        CameraRotation rotation = CameraLookAtRotation.rotation(base.position().value(), target, framing,
                base.fieldOfView().degrees(), ClientViewportAspect.current());
        return new CameraPose(base.position(), rotation, base.fieldOfView());
    }
}
