package io.github.kxyr0.echocutscene.forge.playback.text;

import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetAnchor;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetBinding;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetId;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKeyframe;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKind;
import io.github.kxyr0.echonavigation.api.position.Vector3;

final class CameraTargetLineParser {
    CameraTargetKeyframe target(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid camera target line.");
        }
        long time = Long.parseLong(parts[1]);
        CameraTargetKind kind = CameraTargetKind.valueOf(parts[2]);
        return new CameraTargetKeyframe(time, target(kind, parts));
    }

    private static CameraTarget target(CameraTargetKind kind, String[] parts) {
        return switch (kind) {
            case NONE -> none(parts);
            case FIXED_POINT -> fixed(parts);
            case PLAYER -> player(parts);
            case NPC -> npc(parts);
            case BETWEEN -> between(parts);
        };
    }

    private static CameraTarget none(String[] parts) {
        if (parts.length != 3) {
            throw new IllegalArgumentException("None target does not accept extra data.");
        }
        return CameraTarget.none();
    }

    private static CameraTarget fixed(String[] parts) {
        if (parts.length != 6) {
            throw new IllegalArgumentException("Fixed target requires x y z.");
        }
        return CameraTarget.fixed(new Vector3(number(parts[3]), number(parts[4]), number(parts[5])));
    }

    private static CameraTarget player(String[] parts) {
        if (parts.length != 5) {
            throw new IllegalArgumentException("Player target requires anchor binding.");
        }
        return CameraTarget.player(CameraTargetAnchor.valueOf(parts[3]), CameraTargetBinding.valueOf(parts[4]));
    }

    private static CameraTarget npc(String[] parts) {
        if (parts.length != 6) {
            throw new IllegalArgumentException("NPC target requires id anchor binding.");
        }
        return CameraTarget.npc(new CameraTargetId(parts[3]), CameraTargetAnchor.valueOf(parts[4]),
                CameraTargetBinding.valueOf(parts[5]));
    }

    private static CameraTarget between(String[] parts) {
        if (parts.length != 6) {
            throw new IllegalArgumentException("Between target requires first second weight.");
        }
        return CameraTarget.between(new CameraTargetId(parts[3]), new CameraTargetId(parts[4]),
                number(parts[5]));
    }

    private static double number(String value) {
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed)) {
            throw new IllegalArgumentException("Camera target value must be finite.");
        }
        return parsed;
    }
}
