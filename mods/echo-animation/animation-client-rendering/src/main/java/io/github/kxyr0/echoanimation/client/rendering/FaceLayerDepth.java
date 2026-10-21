package io.github.kxyr0.echoanimation.client.rendering;

import io.github.kxyr0.echoanimation.api.model.LayerRole;
import java.util.Locale;
import java.util.Objects;

/** Keeps overlapping face surfaces distinct after float conversion in the world renderer. */
public final class FaceLayerDepth {
    private static final double LAYER_GAP = 0.001D;
    private static final double AUTHORED_DEPTH_SCALE = 0.001D;

    private FaceLayerDepth() { }

    public static double resolve(double faceZ, LayerRole role, String layerId, double authoredDepth) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(layerId, "layerId");
        int plane = switch (role) {
            case SKIN, DECORATION -> 0;
            case PUPIL -> 1;
            case EYELID -> layerId.toLowerCase(Locale.ROOT).startsWith("lower_eyelid") ? 3 : 2;
            case BROW -> 4;
            case GLASSES -> 5;
        };
        return faceZ - plane * LAYER_GAP - Math.max(authoredDepth, 0.0D) * AUTHORED_DEPTH_SCALE;
    }
}
