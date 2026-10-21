package io.github.kxyr0.echocutscene.core.recording;

import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.camera.framing.CameraFramingKeyframe;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraModeKeyframe;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKeyframe;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKind;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class CameraTrackSourceFormatter {
    public String format(CameraTrackSnapshot snapshot) {
        CameraTrackSnapshot checked = Objects.requireNonNull(snapshot, "snapshot");
        StringBuilder source = new StringBuilder();
        source.append("# Echo camera track ").append(checked.metadata().trackId()).append('\n');
        source.append("schema ").append(checked.metadata().schemaVersion()).append('\n');
        appendModes(source, checked);
        appendTargets(source, checked);
        appendFraming(source, checked);
        source.append("# key <time_ms> <x> <y> <z> <yaw> <pitch> <fov>\n");
        for (CameraKeyframe frame : checked.keyframes()) {
            appendFrame(source, frame);
        }
        return source.toString();
    }

    private static void appendModes(StringBuilder source, CameraTrackSnapshot snapshot) {
        for (CameraModeKeyframe frame : snapshot.modeKeyframes()) {
            source.append("mode ").append(frame.timeMillis()).append(' ').append(frame.mode()).append('\n');
        }
    }

    private static void appendFraming(StringBuilder source, CameraTrackSnapshot snapshot) {
        for (CameraFramingKeyframe frame : snapshot.framingKeyframes()) {
            source.append("framing ").append(frame.timeMillis()).append(' ')
                    .append(number(frame.framing().horizontal())).append(' ')
                    .append(number(frame.framing().vertical())).append('\n');
        }
    }

    private static void appendTargets(StringBuilder source, CameraTrackSnapshot snapshot) {
        for (CameraTargetKeyframe frame : snapshot.targetKeyframes()) {
            source.append("target ").append(frame.timeMillis()).append(' ');
            appendTarget(source, frame.target());
        }
    }

    private static void appendTarget(StringBuilder source, CameraTarget target) {
        switch (target.kind()) {
            case NONE -> source.append("NONE\n");
            case FIXED_POINT -> appendFixed(source, target.position());
            case PLAYER -> source.append("PLAYER ").append(target.anchor()).append(' ')
                    .append(target.binding()).append('\n');
            case NPC -> source.append("NPC ").append(target.primaryId().value()).append(' ')
                    .append(target.anchor()).append(' ').append(target.binding()).append('\n');
            case BETWEEN -> source.append("BETWEEN ").append(target.primaryId().value()).append(' ')
                    .append(target.secondaryId().value()).append(' ').append(number(target.weight())).append('\n');
        }
    }

    private static void appendFixed(StringBuilder source, Vector3 point) {
        source.append("FIXED_POINT ").append(number(point.x())).append(' ')
                .append(number(point.y())).append(' ').append(number(point.z())).append('\n');
    }

    private static void appendFrame(StringBuilder source, CameraKeyframe frame) {
        CameraPose pose = frame.pose();
        Vector3 position = pose.position().value();
        source.append("key ")
                .append(frame.timeMillis()).append(' ')
                .append(number(position.x())).append(' ')
                .append(number(position.y())).append(' ')
                .append(number(position.z())).append(' ')
                .append(number(pose.rotation().yaw())).append(' ')
                .append(number(pose.rotation().pitch())).append(' ')
                .append(number(pose.fieldOfView().degrees())).append('\n');
    }

    private static String number(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
