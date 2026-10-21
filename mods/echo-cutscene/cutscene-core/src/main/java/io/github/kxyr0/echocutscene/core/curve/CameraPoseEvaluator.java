package io.github.kxyr0.echocutscene.core.curve;

import io.github.kxyr0.echocutscene.api.camera.CameraFieldOfView;
import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraPosition;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echocutscene.api.camera.curve.CameraInterpolation;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.List;

public final class CameraPoseEvaluator {
    private int lastIndex;

    public CameraPose evaluate(CompiledCameraCurve curve, long elapsedMillis, double partialTickMillis) {
        double time = Math.max(0.0D, elapsedMillis + partialTickMillis);
        time = Math.min(time, curve.durationMillis());
        List<CameraKeyframe> frames = curve.keyframes();
        lastIndex = KeyframeSearch.floorIndex(frames, time, lastIndex);
        CameraKeyframe a = frames.get(lastIndex);
        CameraKeyframe b = frames.get(Math.min(lastIndex + 1, frames.size() - 1));
        double span = Math.max(1.0D, b.timeMillis() - a.timeMillis());
        double t = Easing.apply(a.interpolation(), (time - a.timeMillis()) / span);
        return pose(frames, lastIndex, a, b, t);
    }

    private CameraPose pose(List<CameraKeyframe> frames, int index, CameraKeyframe a, CameraKeyframe b, double t) {
        Vector3 position = a.interpolation() == CameraInterpolation.CATMULL_ROM
                ? catmull(frames, index, t) : lerp(a.pose().position().value(), b.pose().position().value(), t);
        CameraRotation rotation = rotation(a.pose().rotation(), b.pose().rotation(), t);
        double fov = lerp(a.pose().fieldOfView().degrees(), b.pose().fieldOfView().degrees(), t);
        return new CameraPose(new CameraPosition(position), rotation, new CameraFieldOfView(fov));
    }

    private static CameraRotation rotation(CameraRotation a, CameraRotation b, double t) {
        return new CameraRotation(lerp(a.yaw(), b.yaw(), t), lerp(a.pitch(), b.pitch(), t),
                lerp(a.roll(), b.roll(), t));
    }

    private static Vector3 catmull(List<CameraKeyframe> frames, int index, double t) {
        Vector3 p0 = frames.get(Math.max(0, index - 1)).pose().position().value();
        Vector3 p1 = frames.get(index).pose().position().value();
        Vector3 p2 = frames.get(Math.min(frames.size() - 1, index + 1)).pose().position().value();
        Vector3 p3 = frames.get(Math.min(frames.size() - 1, index + 2)).pose().position().value();
        return new Vector3(cat(p0.x(), p1.x(), p2.x(), p3.x(), t),
                cat(p0.y(), p1.y(), p2.y(), p3.y(), t), cat(p0.z(), p1.z(), p2.z(), p3.z(), t));
    }

    private static double cat(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5D * ((2.0D * p1) + (-p0 + p2) * t
                + (2.0D * p0 - 5.0D * p1 + 4.0D * p2 - p3) * t2
                + (-p0 + 3.0D * p1 - 3.0D * p2 + p3) * t3);
    }

    private static Vector3 lerp(Vector3 a, Vector3 b, double t) {
        return new Vector3(lerp(a.x(), b.x(), t), lerp(a.y(), b.y(), t), lerp(a.z(), b.z(), t));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
