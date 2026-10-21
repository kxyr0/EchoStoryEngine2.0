package io.github.kxyr0.echocutscene.forge.playback.text;

import io.github.kxyr0.echocutscene.api.camera.CameraFieldOfView;
import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraPosition;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echocutscene.api.camera.curve.CameraInterpolation;
import io.github.kxyr0.echocutscene.api.camera.framing.CameraFraming;
import io.github.kxyr0.echocutscene.api.camera.framing.CameraFramingKeyframe;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraModeKeyframe;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKeyframe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CameraTrackTextParser {
    private final CameraTargetLineParser targetParser = new CameraTargetLineParser();

    public ParsedTrack parse(List<String> lines) {
        MutableTrack track = new MutableTrack();
        for (String line : lines) {
            parseLine(line, track);
        }
        int schema = track.schemaVersion > 1 || track.hasSemanticData() ? 2 : 1;
        return new ParsedTrack(sortedKeys(track.keys), sortedModes(track.modes),
                sortedTargets(track.targets), sortedFramings(track.framings), schema);
    }

    private void parseLine(String line, MutableTrack track) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        String[] parts = trimmed.split("\\s+");
        switch (parts[0]) {
            case "schema" -> track.schemaVersion = Integer.parseInt(parts[1]);
            case "key" -> track.keys.add(key(parts));
            case "mode" -> track.modes.add(mode(parts));
            case "target" -> track.targets.add(targetParser.target(parts));
            case "framing" -> track.framings.add(framing(parts));
            default -> throw new IllegalArgumentException("Unknown camera track line: " + parts[0]);
        }
    }

    private static CameraKeyframe key(String[] parts) {
        if (parts.length < 8) {
            throw new IllegalArgumentException("Invalid camera key line.");
        }
        CameraPosition position = CameraPosition.of(number(parts[2]), number(parts[3]), number(parts[4]));
        CameraRotation rotation = new CameraRotation(number(parts[5]), number(parts[6]), 0.0D);
        CameraPose pose = new CameraPose(position, rotation, new CameraFieldOfView(number(parts[7])));
        return new CameraKeyframe(Long.parseLong(parts[1]), pose, CameraInterpolation.SMOOTH);
    }

    private static CameraModeKeyframe mode(String[] parts) {
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid camera mode line.");
        }
        return new CameraModeKeyframe(Long.parseLong(parts[1]), CameraControlMode.valueOf(parts[2]));
    }

    private static CameraFramingKeyframe framing(String[] parts) {
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid camera framing line.");
        }
        return new CameraFramingKeyframe(Long.parseLong(parts[1]),
                new CameraFraming(number(parts[2]), number(parts[3])));
    }

    private static List<CameraKeyframe> sortedKeys(List<CameraKeyframe> frames) {
        return frames.stream().sorted(Comparator.comparingLong(CameraKeyframe::timeMillis)).toList();
    }

    private static List<CameraModeKeyframe> sortedModes(List<CameraModeKeyframe> frames) {
        return frames.stream().sorted(Comparator.comparingLong(CameraModeKeyframe::timeMillis)).toList();
    }

    private static List<CameraTargetKeyframe> sortedTargets(List<CameraTargetKeyframe> frames) {
        return frames.stream().sorted(Comparator.comparingLong(CameraTargetKeyframe::timeMillis)).toList();
    }

    private static List<CameraFramingKeyframe> sortedFramings(List<CameraFramingKeyframe> frames) {
        return frames.stream().sorted(Comparator.comparingLong(CameraFramingKeyframe::timeMillis)).toList();
    }

    private static double number(String value) {
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed)) {
            throw new IllegalArgumentException("Camera track value must be finite.");
        }
        return parsed;
    }

    public record ParsedTrack(
            List<CameraKeyframe> keyframes,
            List<CameraModeKeyframe> modeKeyframes,
            List<CameraTargetKeyframe> targetKeyframes,
            List<CameraFramingKeyframe> framingKeyframes,
            int schemaVersion
    ) {
    }

    private static final class MutableTrack {
        private final List<CameraKeyframe> keys = new ArrayList<>();
        private final List<CameraModeKeyframe> modes = new ArrayList<>();
        private final List<CameraTargetKeyframe> targets = new ArrayList<>();
        private final List<CameraFramingKeyframe> framings = new ArrayList<>();
        private int schemaVersion = 1;

        private boolean hasSemanticData() {
            return !modes.isEmpty() || !targets.isEmpty() || !framings.isEmpty();
        }
    }
}
