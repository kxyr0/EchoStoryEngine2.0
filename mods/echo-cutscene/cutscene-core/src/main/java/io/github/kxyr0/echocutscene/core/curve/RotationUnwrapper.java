package io.github.kxyr0.echocutscene.core.curve;

import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import java.util.ArrayList;
import java.util.List;

final class RotationUnwrapper {
    private RotationUnwrapper() {
    }

    static List<CameraKeyframe> unwrap(List<CameraKeyframe> frames) {
        List<CameraKeyframe> output = new ArrayList<>();
        double previousYaw = frames.get(0).pose().rotation().yaw();
        output.add(frames.get(0));
        for (int i = 1; i < frames.size(); i++) {
            CameraKeyframe frame = frames.get(i);
            double yaw = nearest(previousYaw, frame.pose().rotation().yaw());
            output.add(withYaw(frame, yaw));
            previousYaw = yaw;
        }
        return List.copyOf(output);
    }

    private static double nearest(double previous, double current) {
        double adjusted = current;
        while (adjusted - previous > 180.0D) {
            adjusted -= 360.0D;
        }
        while (adjusted - previous < -180.0D) {
            adjusted += 360.0D;
        }
        return adjusted;
    }

    private static CameraKeyframe withYaw(CameraKeyframe frame, double yaw) {
        CameraRotation old = frame.pose().rotation();
        CameraPose pose = new CameraPose(frame.pose().position(),
                new CameraRotation(yaw, old.pitch(), old.roll()), frame.pose().fieldOfView());
        return new CameraKeyframe(frame.timeMillis(), pose, frame.interpolation());
    }
}
