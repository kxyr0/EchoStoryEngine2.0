package io.github.kxyr0.echocutscene.forge.playback;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackMetadata;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.definition.CutsceneDefinition;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackDataId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackRevision;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneHudPolicy;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneInputPolicy;
import io.github.kxyr0.echocutscene.forge.playback.text.CameraTrackTextParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class CutsceneResourceLoader {
    private final CameraTrackTextParser parser = new CameraTrackTextParser();

    public LoadedCutscenes load(Path root) throws IOException {
        Map<CutsceneId, CutsceneDefinition> definitions = new HashMap<>();
        Map<CameraTrackId, CameraTrackSnapshot> tracks = new HashMap<>();
        if (!Files.isDirectory(root)) {
            return new LoadedCutscenes(definitions, tracks);
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.filter(value -> value.toString().endsWith(".camera.es")).toList()) {
                LoadedTrack loaded = parse(root, path);
                tracks.put(loaded.snapshot.metadata().trackId(), loaded.snapshot);
                definitions.put(loaded.definition.cutsceneId(), loaded.definition);
            }
        }
        return new LoadedCutscenes(definitions, tracks);
    }

    private LoadedTrack parse(Path root, Path path) throws IOException {
        CutsceneId cutsceneId = CutsceneId.parse("jdh:" + strip(root, path));
        CameraTrackId trackId = CameraTrackId.parse(cutsceneId.value() + "_camera");
        CameraTrackTextParser.ParsedTrack parsed = parser.parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        if (parsed.keyframes().size() < 2) {
            throw new IllegalArgumentException("Camera track needs at least two keyframes: " + path);
        }
        CameraTrackRevision revision = new CameraTrackRevision(Files.getLastModifiedTime(path).toMillis());
        long duration = parsed.keyframes().get(parsed.keyframes().size() - 1).timeMillis();
        CameraTrackSnapshot snapshot = new CameraTrackSnapshot(new CameraTrackMetadata(trackId, revision,
                new CameraTrackDataId(root.relativize(path).toString().replace('\\', '/')),
                duration, 60, parsed.schemaVersion(), Long.toHexString(duration)),
                parsed.keyframes(), parsed.modeKeyframes(), parsed.targetKeyframes(), parsed.framingKeyframes());
        return new LoadedTrack(new CutsceneDefinition(cutsceneId, trackId, revision, 1.0D,
                CutsceneInputPolicy.defaultPlayback(), CutsceneHudPolicy.defaultPlayback()), snapshot);
    }

    private static String strip(Path root, Path path) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        return relative.substring(0, relative.length() - ".camera.es".length());
    }

    public record LoadedCutscenes(Map<CutsceneId, CutsceneDefinition> definitions,
            Map<CameraTrackId, CameraTrackSnapshot> tracks) {
    }

    private record LoadedTrack(CutsceneDefinition definition, CameraTrackSnapshot snapshot) {
    }
}
