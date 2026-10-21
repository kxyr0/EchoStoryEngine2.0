package io.github.kxyr0.echocutscene.forge.client.authoring;

import io.github.kxyr0.echocutscene.api.camera.framing.CameraFraming;
import io.github.kxyr0.echocutscene.api.camera.framing.CameraFramingKeyframe;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraModeKeyframe;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetAnchor;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetBinding;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKeyframe;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKind;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ClientAuthoringSemanticState {
    private static final double FRAMING_SCALE = 1.0D / 45.0D;
    private final List<CameraModeKeyframe> recordedModes = new ArrayList<>();
    private final List<CameraTargetKeyframe> recordedTargets = new ArrayList<>();
    private final List<CameraFramingKeyframe> recordedFramings = new ArrayList<>();
    private CameraControlMode mode = CameraControlMode.FREE;
    private CameraTarget target = CameraTarget.player(CameraTargetAnchor.EYES, CameraTargetBinding.LIVE);
    private CameraFraming framing = CameraFraming.CENTER;
    private CameraFraming lastRecordedFraming = CameraFraming.CENTER;
    private long recordingStartedAtMillis = -1L;

    public void reset() {
        mode = CameraControlMode.FREE;
        target = CameraTarget.player(CameraTargetAnchor.EYES, CameraTargetBinding.LIVE);
        framing = CameraFraming.CENTER;
        lastRecordedFraming = framing;
        recordingStartedAtMillis = -1L;
        recordedModes.clear();
        recordedTargets.clear();
        recordedFramings.clear();
    }

    public CameraControlMode cycleMode(long nowMillis) {
        setMode(mode.next(), nowMillis);
        return mode;
    }

    public void setMode(CameraControlMode nextMode, long nowMillis) {
        if (mode == nextMode) {
            return;
        }
        mode = nextMode;
        recordMode(nowMillis);
    }

    public void setTarget(CameraTarget nextTarget, long nowMillis) {
        target = nextTarget;
        recordTarget(nowMillis);
    }

    public void clearTarget(long nowMillis) {
        target = CameraTarget.none();
        recordTarget(nowMillis);
        setMode(CameraControlMode.FREE, nowMillis);
    }

    public void adjustFraming(double yawDelta, double pitchDelta) {
        framing = framing.addClamped(yawDelta * FRAMING_SCALE, pitchDelta * FRAMING_SCALE);
    }

    public void beginRecording(long nowMillis) {
        recordingStartedAtMillis = nowMillis;
        recordedModes.clear();
        recordedTargets.clear();
        recordedFramings.clear();
        recordMode(nowMillis);
        recordTarget(nowMillis);
        recordFraming(nowMillis);
    }

    public void recordFramingSample(long nowMillis) {
        if (recordingStartedAtMillis >= 0L && !framing.equals(lastRecordedFraming)) {
            recordFraming(nowMillis);
        }
    }

    public void endRecording() {
        recordingStartedAtMillis = -1L;
    }

    public CameraControlMode mode() {
        return mode;
    }

    public Optional<CameraTarget> target() {
        if (target == null || target.kind() == CameraTargetKind.NONE) {
            return Optional.empty();
        }
        return Optional.ofNullable(target);
    }

    public CameraFraming framing() {
        return framing;
    }

    public List<CameraModeKeyframe> recordedModes() {
        return List.copyOf(recordedModes);
    }

    public List<CameraTargetKeyframe> recordedTargets() {
        return List.copyOf(recordedTargets);
    }

    public List<CameraFramingKeyframe> recordedFramings() {
        return List.copyOf(recordedFramings);
    }

    public String targetLabel() {
        if (target == null || target.kind() == CameraTargetKind.NONE) {
            return "none";
        }
        if (target.kind() == CameraTargetKind.FIXED_POINT) {
            return "FIXED_POINT " + point(target.position());
        }
        return target.kind() + "/" + target.anchor() + "/" + target.binding();
    }

    private static String point(Vector3 point) {
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", point.x(), point.y(), point.z());
    }

    private void recordMode(long nowMillis) {
        if (recordingStartedAtMillis >= 0L) {
            recordedModes.add(new CameraModeKeyframe(relative(nowMillis), mode));
        }
    }

    private void recordTarget(long nowMillis) {
        if (recordingStartedAtMillis >= 0L && target != null) {
            recordedTargets.add(new CameraTargetKeyframe(relative(nowMillis), target));
        }
    }

    private void recordFraming(long nowMillis) {
        if (recordingStartedAtMillis >= 0L) {
            recordedFramings.add(new CameraFramingKeyframe(relative(nowMillis), framing));
            lastRecordedFraming = framing;
        }
    }

    private long relative(long nowMillis) {
        return Math.max(0L, nowMillis - recordingStartedAtMillis);
    }
}
