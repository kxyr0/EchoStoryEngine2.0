package io.github.kxyr0.echocutscene.forge.client.authoring;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraPosition;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.forge.client.ClientCameraMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class ClientAuthoringInput {
    private double yaw;
    private double pitch;

    public void set(CameraPose pose) {
        yaw = pose.rotation().yaw();
        pitch = pose.rotation().pitch();
    }

    public void reset() { yaw = 0.0D; pitch = 0.0D; }

    public void absorb(LocalPlayer player, double bodyYaw, double bodyPitch, ClientAuthoringSemanticState semantic) {
        double yawDelta = Mth.wrapDegrees(player.getYRot() - (float) bodyYaw);
        double pitchDelta = player.getXRot() - bodyPitch;
        if (semantic.mode() == CameraControlMode.FREE) {
            yaw += yawDelta;
            pitch = Math.max(-90.0D, Math.min(90.0D, pitch + pitchDelta));
        } else if (semantic.mode() == CameraControlMode.LOOK_AT_WITH_OFFSET) {
            semantic.adjustFraming(yawDelta, pitchDelta);
        }
    }

    public CameraPose orient(CameraPose pose, ClientAuthoringSemanticState semantic,
            ClientAuthoringOrientation orientation) {
        return orientation.orient(pose, semantic, yaw, pitch);
    }

    public CameraPose move(CameraPose pose, ClientAuthoringSemanticState semantic,
            ClientAuthoringOrientation orientation, Minecraft minecraft, Timing timing) {
        CameraPose oriented = orient(pose, semantic, orientation);
        var position = pose.position().value();
        var delta = ClientCameraMovement.delta(minecraft, oriented.rotation(), timing.frameSeconds(),
                timing.speed(), timing.pitchFlight());
        var moved = new CameraPose(CameraPosition.of(position.x() + delta.x(), position.y() + delta.y(),
                position.z() + delta.z()), oriented.rotation(), pose.fieldOfView());
        CameraPose result = orient(moved, semantic, orientation);
        set(result);
        return result;
    }

    public record Timing(double frameSeconds, double speed, boolean pitchFlight) { }
}
