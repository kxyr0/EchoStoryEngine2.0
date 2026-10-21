package io.github.kxyr0.echoik.debug;

import io.github.kxyr0.echoik.core.math.Quat;
import io.github.kxyr0.echoik.core.math.Vec3;

/**
 * Platform-neutral debug draw commands.
 */
public sealed interface DebugPrimitive permits
        DebugPrimitive.Line,
        DebugPrimitive.Point,
        DebugPrimitive.Axis,
        DebugPrimitive.Text {
    record Line(Vec3 from, Vec3 to, String label) implements DebugPrimitive {
    }

    record Point(Vec3 position, String label) implements DebugPrimitive {
    }

    record Axis(Vec3 origin, Quat rotation, double length, String label) implements DebugPrimitive {
    }

    record Text(Vec3 position, String text) implements DebugPrimitive {
    }
}
