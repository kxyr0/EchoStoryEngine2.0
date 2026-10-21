package io.github.kxyr0.echoanimation.api.clip.definition;

import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import java.util.Objects;

/**
 * Named, zero-copy view over a tick range of one animation clip.
 */
public record AnimationClipSegment(
        String id,
        int startTick,
        int endTick,
        AnimationLoopSeam loopSeam,
        double authoredSpeedBlocksPerSecond
) {
    public AnimationClipSegment(String id, int startTick, int endTick) {
        this(id, startTick, endTick, AnimationLoopSeam.HOLD, 1.0D);
    }

    public AnimationClipSegment {
        id = Objects.requireNonNull(id, "id").trim();
        loopSeam = Objects.requireNonNull(loopSeam, "loopSeam");
        if (id.isEmpty() || !id.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Segment id contains invalid characters: " + id);
        }
        if (startTick < 0 || startTick >= endTick) {
            throw new IllegalArgumentException("Segment '" + id + "' references ticks " + startTick + ".."
                    + endTick + ": start must be before end.");
        }
        if (!Double.isFinite(authoredSpeedBlocksPerSecond) || authoredSpeedBlocksPerSecond <= 0.0D) {
            throw new IllegalArgumentException("Segment speed must be finite and positive.");
        }
    }

    public int durationTicks() {
        return endTick - startTick;
    }
}
