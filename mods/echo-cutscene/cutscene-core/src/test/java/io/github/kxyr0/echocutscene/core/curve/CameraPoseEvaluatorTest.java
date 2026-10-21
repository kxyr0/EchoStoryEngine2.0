package io.github.kxyr0.echocutscene.core.curve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.kxyr0.echocutscene.api.camera.CameraFieldOfView;
import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraPosition;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackMetadata;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.camera.curve.CameraInterpolation;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackDataId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackRevision;
import java.util.List;
import org.junit.jupiter.api.Test;

class CameraPoseEvaluatorTest {
    private final CameraCurveCompiler compiler = new CameraCurveCompiler();

    @Test
    void linearInterpolationIsDeterministic() {
        CameraPose pose = evaluate(track(CameraInterpolation.LINEAR), 500L, 0.0D);

        assertEquals(5.0D, pose.position().value().x(), 0.0001D);
        assertEquals(45.0D, pose.rotation().yaw(), 0.0001D);
        assertEquals(75.0D, pose.fieldOfView().degrees(), 0.0001D);
    }

    @Test
    void partialTickContributesToSampleTime() {
        CameraPose pose = evaluate(track(CameraInterpolation.LINEAR), 400L, 100.0D);

        assertEquals(5.0D, pose.position().value().x(), 0.0001D);
    }

    @Test
    void smoothInterpolationEasesMidpoints() {
        CameraPose pose = evaluate(track(CameraInterpolation.SMOOTH), 250L, 0.0D);

        assertEquals(1.5625D, pose.position().value().x(), 0.0001D);
    }

    @Test
    void yawWrapUsesShortestPath() {
        CameraPose pose = evaluate(wrapTrack(359.0D, 1.0D), 500L, 0.0D);

        assertEquals(360.0D, pose.rotation().yaw(), 0.0001D);
    }

    @Test
    void catmullRomDoesNotProduceNan() {
        CameraPose pose = evaluate(track(CameraInterpolation.CATMULL_ROM), 500L, 0.0D);

        assertFalse(Double.isNaN(pose.position().value().x()));
        assertEquals(5.0D, pose.position().value().x(), 0.0001D);
    }

    private CameraPose evaluate(CameraTrackSnapshot snapshot, long elapsed, double partial) {
        return new CameraPoseEvaluator().evaluate(compiler.compile(snapshot), elapsed, partial);
    }

    private static CameraTrackSnapshot track(CameraInterpolation interpolation) {
        return snapshot(List.of(
                new CameraKeyframe(0L, CameraPose.of(0.0D, 64.0D, 0.0D, 0.0D, 70.0D), interpolation),
                new CameraKeyframe(1000L, tiltedPose(), interpolation)));
    }

    private static CameraTrackSnapshot wrapTrack(double firstYaw, double secondYaw) {
        return snapshot(List.of(
                new CameraKeyframe(0L, CameraPose.of(0.0D, 64.0D, 0.0D, firstYaw, 70.0D),
                        CameraInterpolation.LINEAR),
                new CameraKeyframe(1000L, CameraPose.of(0.0D, 64.0D, 0.0D, secondYaw, 70.0D),
                        CameraInterpolation.LINEAR)));
    }

    private static CameraPose tiltedPose() {
        return new CameraPose(CameraPosition.of(10.0D, 64.0D, 0.0D),
                new CameraRotation(90.0D, 10.0D, 0.0D), new CameraFieldOfView(80.0D));
    }

    private static CameraTrackSnapshot snapshot(List<CameraKeyframe> frames) {
        CameraTrackMetadata metadata = new CameraTrackMetadata(CameraTrackId.parse("jdh:test"),
                new CameraTrackRevision(1L), new CameraTrackDataId("test"), 1000L, 20, 1, "fingerprint");
        return new CameraTrackSnapshot(metadata, frames);
    }
}
