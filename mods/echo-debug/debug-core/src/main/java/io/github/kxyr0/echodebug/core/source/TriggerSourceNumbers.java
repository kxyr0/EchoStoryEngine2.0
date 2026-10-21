package io.github.kxyr0.echodebug.core.source;

import io.github.kxyr0.echotrigger.api.geometry.Vector3;

final class TriggerSourceNumbers {
    private TriggerSourceNumbers() {
    }

    static String vector(Vector3 vector) {
        return "Vector3(" + number(vector.X()) + ", " + number(vector.Y()) + ", " + number(vector.Z()) + ")";
    }

    static String number(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
