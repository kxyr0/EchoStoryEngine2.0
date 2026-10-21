package io.github.kxyr0.echocutscene.forge.playback.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetAnchor;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class CameraTrackTextParserTest {
    private final CameraTrackTextParser parser = new CameraTrackTextParser();

    @Test
    void legacyKeyOnlyTrackParsesAsSchemaOne() {
        CameraTrackTextParser.ParsedTrack track = parser.parse(List.of(
                "key 0 0 64 0 0 0 70",
                "key 1000 1 64 0 90 0 70"));

        assertEquals(1, track.schemaVersion());
        assertEquals(2, track.keyframes().size());
        assertTrue(track.modeKeyframes().isEmpty());
    }

    @Test
    void schemaTwoParsesSemanticTracks() {
        CameraTrackTextParser.ParsedTrack track = parser.parse(List.of(
                "schema 2",
                "mode 0 LOOK_AT_WITH_OFFSET",
                "target 0 PLAYER EYES LIVE",
                "framing 0 0.25 -0.1",
                "key 0 0 64 0 0 0 70",
                "key 1000 1 64 0 90 0 70"));

        assertEquals(2, track.schemaVersion());
        assertEquals(CameraControlMode.LOOK_AT_WITH_OFFSET, track.modeKeyframes().get(0).mode());
        assertEquals(CameraTargetKind.PLAYER, track.targetKeyframes().get(0).target().kind());
        assertEquals(CameraTargetAnchor.EYES, track.targetKeyframes().get(0).target().anchor());
        assertEquals(0.25D, track.framingKeyframes().get(0).framing().horizontal(), 0.0001D);
    }
}
