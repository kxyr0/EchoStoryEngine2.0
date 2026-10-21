package io.github.kxyr0.echocutscene.forge.playback;

import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackMetadata;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.camera.curve.CameraInterpolation;
import io.github.kxyr0.echocutscene.api.definition.CutsceneDefinition;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackDataId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackRevision;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneHudPolicy;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneInputPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ServerCutsceneResourceCatalog {
    private final Map<CutsceneId, CutsceneDefinition> definitions = new HashMap<>();
    private final Map<CameraTrackId, CameraTrackSnapshot> tracks = new HashMap<>();

    public ServerCutsceneResourceCatalog() {
        installBuiltInPreview();
    }

    public Optional<CutsceneDefinition> definition(CutsceneId cutsceneId) {
        return Optional.ofNullable(definitions.get(cutsceneId));
    }

    public List<CutsceneDefinition> definitions() {
        return definitions.values().stream()
                .sorted(java.util.Comparator.comparing(definition -> definition.cutsceneId().toString()))
                .toList();
    }

    public Optional<CameraTrackSnapshot> track(CameraTrackId trackId) {
        return Optional.ofNullable(tracks.get(trackId));
    }

    public List<CameraTrackSnapshot> tracks() {
        return List.copyOf(tracks.values());
    }

    public void replaceAll(Map<CutsceneId, CutsceneDefinition> nextDefinitions,
            Map<CameraTrackId, CameraTrackSnapshot> nextTracks) {
        definitions.clear();
        tracks.clear();
        definitions.putAll(nextDefinitions);
        tracks.putAll(nextTracks);
        installBuiltInPreview();
    }

    private void installBuiltInPreview() {
        CameraTrackId trackId = CameraTrackId.parse("jdh:chapter_1/bedtime_story_camera");
        CameraTrackRevision revision = new CameraTrackRevision(1L);
        CameraTrackMetadata metadata = new CameraTrackMetadata(trackId, revision,
                new CameraTrackDataId("builtin-bedtime-story"), 6000L, 20, 1, "builtin-c1-preview");
        tracks.putIfAbsent(trackId, new CameraTrackSnapshot(metadata, List.of(
                frame(0L, -2.0D, 65.0D, -2.0D, 45.0D),
                frame(3000L, 2.0D, 65.5D, -1.0D, -35.0D),
                frame(6000L, 0.0D, 66.0D, 3.0D, 180.0D))));
        CutsceneId cutsceneId = CutsceneId.parse("jdh:chapter_1/bedtime_story");
        definitions.putIfAbsent(cutsceneId, new CutsceneDefinition(cutsceneId, trackId, revision, 1.0D,
                CutsceneInputPolicy.defaultPlayback(), CutsceneHudPolicy.defaultPlayback()));
    }

    private static CameraKeyframe frame(long time, double x, double y, double z, double yaw) {
        return new CameraKeyframe(time, CameraPose.of(x, y, z, yaw, 70.0D),
                CameraInterpolation.SMOOTH);
    }
}
