package io.github.kxyr0.echocutscene.core.recording;

import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.ArrayList;
import java.util.List;

public final class CameraTrackSimplifier {
    public CameraTrackSnapshot simplify(CameraTrackSnapshot snapshot, double minDistance, double minAngle) {
        List<CameraKeyframe> frames = snapshot.keyframes();
        List<CameraKeyframe> kept = new ArrayList<>();
        kept.add(frames.get(0));
        for (int i = 1; i < frames.size() - 1; i++) {
            if (different(kept.get(kept.size() - 1), frames.get(i), minDistance, minAngle)) {
                kept.add(frames.get(i));
            }
        }
        kept.add(frames.get(frames.size() - 1));
        return new CameraTrackSnapshot(snapshot.metadata(), kept, snapshot.modeKeyframes(),
                snapshot.targetKeyframes(), snapshot.framingKeyframes());
    }

    private static boolean different(CameraKeyframe a, CameraKeyframe b, double minDistance, double minAngle) {
        Vector3 av = a.pose().position().value();
        Vector3 bv = b.pose().position().value();
        double dx = av.x() - bv.x();
        double dy = av.y() - bv.y();
        double dz = av.z() - bv.z();
        double angle = Math.abs(a.pose().rotation().yaw() - b.pose().rotation().yaw())
                + Math.abs(a.pose().rotation().pitch() - b.pose().rotation().pitch());
        return Math.sqrt(dx * dx + dy * dy + dz * dz) >= minDistance || angle >= minAngle;
    }
}
