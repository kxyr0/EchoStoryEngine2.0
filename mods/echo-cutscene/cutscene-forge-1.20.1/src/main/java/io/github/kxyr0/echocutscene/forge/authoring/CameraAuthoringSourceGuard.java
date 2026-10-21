package io.github.kxyr0.echocutscene.forge.authoring;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.forge.playback.text.CameraTrackTextParser;
import java.nio.file.Path;
import java.util.Optional;

final class CameraAuthoringSourceGuard {
    private static final int MAX_SOURCE_LENGTH = 262_144;
    private static final int MAX_KEYFRAMES = 2_000;

    private CameraAuthoringSourceGuard() {
    }

    static Path target(Path root, CutsceneId cutsceneId) {
        Path normalizedRoot = root.normalize();
        Path target = normalizedRoot.resolve(cutscenePath(cutsceneId) + ".camera.es").normalize();
        if (!target.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("cutscene path escapes root");
        }
        return target;
    }

    static Optional<String> validateCutsceneId(CutsceneId cutsceneId) {
        try {
            cutscenePath(cutsceneId);
            return Optional.empty();
        } catch (IllegalArgumentException exception) {
            return Optional.of(exception.getMessage());
        }
    }

    static Optional<String> validateSource(String source) {
        if (source == null || source.isBlank() || source.length() > MAX_SOURCE_LENGTH) {
            return Optional.of("invalid source length");
        }
        try {
            CameraTrackTextParser.ParsedTrack parsed = new CameraTrackTextParser().parse(source.lines().toList());
            return keyframes(parsed.keyframes().size());
        } catch (IllegalArgumentException exception) {
            return Optional.of(exception.getMessage());
        }
    }

    private static String cutscenePath(CutsceneId cutsceneId) {
        if (!cutsceneId.value().startsWith("jdh:")) {
            throw new IllegalArgumentException("filesystem camera tracks require jdh namespace");
        }
        String value = cutsceneId.value().substring(cutsceneId.value().indexOf(':') + 1);
        if (value.endsWith("/")) {
            throw new IllegalArgumentException("invalid cutscene path segment");
        }
        for (String segment : value.split("/")) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("invalid cutscene path segment");
            }
        }
        return value;
    }

    private static Optional<String> keyframes(int keyframes) {
        if (keyframes < 2 || keyframes > MAX_KEYFRAMES) {
            return Optional.of("invalid keyframe count");
        }
        return Optional.empty();
    }
}
