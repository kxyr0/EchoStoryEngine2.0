package io.github.kxyr0.echoanimation.echoscript.procedural.clip;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipSegment;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;

/**
 * Mutable annotations collected for one segment before immutable lowering.
 */
final class ClipSegmentDraft {
    final String id;
    final int start;
    final int end;
    AnimationLoopSeam seam = AnimationLoopSeam.HOLD;
    double speed = 1.0D;
    boolean loopDeclared;

    ClipSegmentDraft(String id, int start, int end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    AnimationClipSegment compile() {
        return new AnimationClipSegment(id, start, end, seam, speed);
    }
}
