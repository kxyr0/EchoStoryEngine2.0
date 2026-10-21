package io.github.kxyr0.echocutscene.core.curve;

import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import java.util.List;

final class KeyframeSearch {
    private KeyframeSearch() {
    }

    static int floorIndex(List<CameraKeyframe> frames, double millis, int previous) {
        int index = Math.max(0, Math.min(previous, frames.size() - 2));
        while (index < frames.size() - 2 && frames.get(index + 1).timeMillis() <= millis) {
            index++;
        }
        while (index > 0 && frames.get(index).timeMillis() > millis) {
            index--;
        }
        return index;
    }
}
