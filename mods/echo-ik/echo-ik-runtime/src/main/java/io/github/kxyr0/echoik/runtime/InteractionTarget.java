package io.github.kxyr0.echoik.runtime;

import io.github.kxyr0.echoik.core.math.Quat;
import io.github.kxyr0.echoik.core.math.Vec3;
import java.util.Objects;
import java.util.Optional;

/**
 * Interaction-level target data. These values are resolved before entering core IK.
 */
public sealed interface InteractionTarget permits
        InteractionTarget.WorldTarget,
        InteractionTarget.BoneTarget,
        InteractionTarget.SurfaceTarget,
        InteractionTarget.SocketTarget {
    String id();

    Optional<Quat> rotation();

    double weight();

    record WorldTarget(String id, Vec3 position, Optional<Quat> rotation, double weight)
            implements InteractionTarget {
        public WorldTarget {
            id = required(id, "id");
            position = Objects.requireNonNull(position, "position");
            rotation = Objects.requireNonNull(rotation, "rotation");
            weight = checkedWeight(weight);
        }
    }

    record BoneTarget(String id, String entityId, String boneId, Vec3 offset, Optional<Quat> rotation, double weight)
            implements InteractionTarget {
        public BoneTarget {
            id = required(id, "id");
            entityId = required(entityId, "entityId");
            boneId = required(boneId, "boneId");
            offset = Objects.requireNonNull(offset, "offset");
            rotation = Objects.requireNonNull(rotation, "rotation");
            weight = checkedWeight(weight);
        }
    }

    record SurfaceTarget(String id, Vec3 point, Vec3 normal, Optional<Quat> rotation, double weight)
            implements InteractionTarget {
        public SurfaceTarget {
            id = required(id, "id");
            point = Objects.requireNonNull(point, "point");
            normal = Objects.requireNonNull(normal, "normal").normalize();
            rotation = Objects.requireNonNull(rotation, "rotation");
            weight = checkedWeight(weight);
        }
    }

    record SocketTarget(String id, String socketId, Vec3 position, Optional<Quat> rotation, double weight)
            implements InteractionTarget {
        public SocketTarget {
            id = required(id, "id");
            socketId = required(socketId, "socketId");
            position = Objects.requireNonNull(position, "position");
            rotation = Objects.requireNonNull(rotation, "rotation");
            weight = checkedWeight(weight);
        }
    }

    private static String required(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }

    private static double checkedWeight(double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException("weight must be in range 0..1.");
        }
        return value;
    }
}
