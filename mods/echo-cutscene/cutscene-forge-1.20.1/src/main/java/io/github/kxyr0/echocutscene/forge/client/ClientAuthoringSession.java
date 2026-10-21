package io.github.kxyr0.echocutscene.forge.client;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.framing.CameraFraming;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.forge.client.authoring.ClientAuthoringOrientation;
import io.github.kxyr0.echocutscene.forge.client.authoring.ClientAuthoringRecording;
import io.github.kxyr0.echocutscene.forge.client.authoring.ClientAuthoringSemanticState;
import io.github.kxyr0.echocutscene.forge.client.authoring.ClientAuthoringTargetRaycast;
import io.github.kxyr0.echocutscene.forge.client.authoring.ClientFrameClock;
import io.github.kxyr0.echocutscene.forge.client.body.ClientLocalBodyLock;
import java.util.OptionalDouble;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

final class ClientAuthoringSession {
    private final ClientCameraRig rig = new ClientCameraRig();
    private final ClientAuthoringSemanticState semantic = new ClientAuthoringSemanticState();
    private final ClientAuthoringOrientation orientation = new ClientAuthoringOrientation();
    private final ClientAuthoringRecording recorder = new ClientAuthoringRecording();
    private final ClientAuthoringTargetRaycast targetRaycast = new ClientAuthoringTargetRaycast();
    private final ClientFrameClock frameClock = new ClientFrameClock();
    private String sessionId;
    private CutsceneId cutsceneId;
    private CameraPose pose;
    private double bodyYaw;
    private double bodyPitch;
    private final io.github.kxyr0.echocutscene.forge.client.authoring.ClientAuthoringInput input =
            new io.github.kxyr0.echocutscene.forge.client.authoring.ClientAuthoringInput();
    private double cameraSpeed;
    private boolean pitchFlight;

    void begin(
            String nextSessionId,
            CutsceneId nextCutsceneId,
            CameraPose initialPose,
            double nextCameraSpeed) {
        sessionId = nextSessionId;
        cutsceneId = nextCutsceneId;
        pose = initialPose;
        bodyYaw = initialPose.rotation().yaw();
        bodyPitch = initialPose.rotation().pitch();
        input.set(initialPose);
        cameraSpeed = nextCameraSpeed;
        pitchFlight = false;
        semantic.reset();
        recorder.reset();
        frameClock.reset(Util.getMillis());
        ClientLocalBodyLock.apply(bodyYaw, bodyPitch);
        rig.detach(initialPose);
    }

    void end(String closedSessionId) {
        if (sessionId == null || sessionId.equals(closedSessionId)) {
            clear();
        }
    }

    void renderTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || minecraft.player == null) {
            return;
        }
        double frameSeconds = frameClock.seconds(Util.getMillis());
        if (minecraft.screen == null) {
            input.absorb(minecraft.player, bodyYaw, bodyPitch, semantic);
            pose = input.move(pose, semantic, orientation, minecraft,
                    new io.github.kxyr0.echocutscene.forge.client.authoring.ClientAuthoringInput.Timing(
                            frameSeconds, cameraSpeed, pitchFlight));
        }
        ClientLocalBodyLock.apply(bodyYaw, bodyPitch);
        rig.apply(pose);
        recorder.sample(Util.getMillis(), pose, semantic);
    }

    void toggleRecording() {
        if (!active()) {
            return;
        }
        recorder.toggle(sessionId, cutsceneId, pose, semantic);
    }

    void togglePitchFlight() {
        pitchFlight = !pitchFlight;
    }

    void cycleCameraMode() {
        if (active()) {
            semantic.cycleMode(Util.getMillis());
            syncFreeRotation();
        }
    }

    void setFixedTargetFromView() {
        if (!active() || pose == null) {
            return;
        }
        captureCurrentViewDirection();
        targetRaycast
                .fixedTarget(pose)
                .ifPresent(
                        target -> {
                            semantic.setTarget(target, Util.getMillis());
                            pose = input.orient(pose, semantic, orientation);
                            input.set(pose);
                            rig.apply(pose);
                        });
    }

    void setCameraMode(String changedSessionId, CameraControlMode mode) {
        if (sessionId != null && sessionId.equals(changedSessionId)) {
            semantic.setMode(mode, Util.getMillis());
            syncFreeRotation();
        }
    }

    void setTarget(String changedSessionId, CameraTarget target) {
        if (sessionId != null && sessionId.equals(changedSessionId)) {
            semantic.setTarget(target, Util.getMillis());
        }
    }

    void clearTarget(String changedSessionId) {
        if (sessionId != null && sessionId.equals(changedSessionId)) {
            semantic.clearTarget(Util.getMillis());
        }
    }

    boolean active() {
        return sessionId != null;
    }

    boolean recording() {
        return recorder.active();
    }

    int sampleCount() {
        return recorder.sampleCount();
    }

    double cameraSpeed() {
        return cameraSpeed;
    }

    boolean pitchFlight() {
        return pitchFlight;
    }

    CameraControlMode cameraMode() {
        return semantic.mode();
    }

    CameraFraming framing() {
        return semantic.framing();
    }

    String targetLabel() {
        return semantic.targetLabel();
    }

    String cutsceneLabel() {
        return cutsceneId == null ? "none" : cutsceneId.value();
    }

    OptionalDouble currentFov() {
        return pose == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(pose.fieldOfView().degrees());
    }

    private void syncFreeRotation() {
        if (pose != null) {
            input.set(pose);
        }
    }

    private void captureCurrentViewDirection() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        input.absorb(minecraft.player, bodyYaw, bodyPitch, semantic);
        pose = input.orient(pose, semantic, orientation);
        input.set(pose);
        ClientLocalBodyLock.apply(bodyYaw, bodyPitch);
    }

    private void clear() {
        rig.attachToBody();
        sessionId = null;
        cutsceneId = null;
        pose = null;
        recorder.reset();
        bodyYaw = 0.0D;
        bodyPitch = 0.0D;
        input.reset();
        pitchFlight = false;
        semantic.reset();
        frameClock.reset(0L);
    }

}
