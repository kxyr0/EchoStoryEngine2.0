package io.github.kxyr0.echoanimation.echoscript.face;

import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Vec2;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parsed high-level face rig model before lowering.
 */
final class FaceScriptModel {
    enum PartKind {
        SCLERA, PUPIL, EYELID, BROW
    }

    record Part(PartKind kind, String variant, Settings settings) {
    }

    record ExtraBone(String name, Optional<String> parent, Settings settings) {
    }

    static final class Face {
        final String name;
        final String id;
        Optional<String> textureRoot = Optional.empty();
        final List<Part> parts = new ArrayList<>();
        final List<ExtraBone> bones = new ArrayList<>();
        final List<Blink> blinks = new ArrayList<>();

        Face(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }

    static final class Blink {
        final String id;
        Optional<Vec2> travel = Optional.empty();
        Optional<Vec2> gaze = Optional.empty();
        Optional<Boolean> idle = Optional.empty();
        Optional<Timing> timing = Optional.empty();

        Blink(String id) {
            this.id = id;
        }
    }

    static final class Settings {
        Optional<Vec3> position = Optional.empty();
        Optional<Vec3> pivot = Optional.empty();
        Optional<String> texture = Optional.empty();
        Optional<IntRect> textureArea = Optional.empty();
        Optional<Size> size = Optional.empty();
        Optional<Integer> order = Optional.empty();
        Optional<String> bone = Optional.empty();
        Optional<LayerRole> role = Optional.empty();

        Settings copy() {
            Settings result = new Settings();
            result.position = position;
            result.pivot = pivot;
            result.texture = texture;
            result.textureArea = textureArea;
            result.size = size;
            result.order = order;
            result.bone = bone;
            result.role = role;
            return result;
        }
    }

    record Size(int width, int height) {
    }

    record Timing(int periodTicks, int closeTicks, int holdTicks, int openTicks) {
    }

    private FaceScriptModel() {
    }
}
