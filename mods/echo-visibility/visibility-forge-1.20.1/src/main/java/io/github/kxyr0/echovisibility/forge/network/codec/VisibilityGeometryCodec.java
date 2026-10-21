package io.github.kxyr0.echovisibility.forge.network.codec;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityDimensionId;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityProjectionMatrix;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.policy.VisibilityViewpoint;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.api.target.VisibilityTargetBounds;
import net.minecraft.network.FriendlyByteBuf;

public final class VisibilityGeometryCodec {
    private VisibilityGeometryCodec() {
    }

    public static void writeVector(FriendlyByteBuf buffer, VisibilityVector3 value) {
        buffer.writeDouble(value.x());
        buffer.writeDouble(value.y());
        buffer.writeDouble(value.z());
    }

    public static VisibilityVector3 readVector(FriendlyByteBuf buffer) {
        return new VisibilityVector3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void writeBounds(FriendlyByteBuf buffer, VisibilityTargetBounds value) {
        buffer.writeUtf(value.dimension().value());
        writeVector(buffer, value.bounds().min());
        writeVector(buffer, value.bounds().max());
        buffer.writeLong(value.revision().value());
    }

    public static VisibilityTargetBounds readBounds(FriendlyByteBuf buffer) {
        VisibilityDimensionId dimension = new VisibilityDimensionId(buffer.readUtf());
        VisibilityAabb bounds = new VisibilityAabb(readVector(buffer), readVector(buffer));
        return new VisibilityTargetBounds(dimension, bounds,
                new VisibilityDefinitionRevision(buffer.readLong()));
    }

    public static void writeCamera(FriendlyByteBuf buffer, VisibilityCameraSnapshot value) {
        buffer.writeUtf(value.source());
        buffer.writeEnum(value.viewpoint());
        writeVector(buffer, value.position());
        writeVector(buffer, value.forward());
        writeVector(buffer, value.up());
        writeVector(buffer, value.right());
        buffer.writeDouble(value.yaw());
        buffer.writeDouble(value.pitch());
        for (double component : value.projection().values()) {
            buffer.writeDouble(component);
        }
        buffer.writeVarInt(value.framebufferWidth());
        buffer.writeVarInt(value.framebufferHeight());
        buffer.writeLong(value.projectionRevision());
        writeVector(buffer, value.playerEyePosition());
        writeVector(buffer, value.playerForward());
        buffer.writeDouble(value.playerYaw());
        buffer.writeDouble(value.playerPitch());
    }

    public static VisibilityCameraSnapshot readCamera(FriendlyByteBuf buffer) {
        String source = buffer.readUtf();
        VisibilityViewpoint viewpoint = buffer.readEnum(VisibilityViewpoint.class);
        VisibilityVector3 position = readVector(buffer);
        VisibilityVector3 forward = readVector(buffer);
        VisibilityVector3 up = readVector(buffer);
        VisibilityVector3 right = readVector(buffer);
        double yaw = buffer.readDouble();
        double pitch = buffer.readDouble();
        double[] projection = new double[16];
        for (int index = 0; index < projection.length; index++) {
            projection[index] = buffer.readDouble();
        }
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        long revision = buffer.readLong();
        VisibilityVector3 playerEyes = readVector(buffer);
        VisibilityVector3 playerForward = readVector(buffer);
        double playerYaw = buffer.readDouble();
        double playerPitch = buffer.readDouble();
        return new VisibilityCameraSnapshot(source, viewpoint, position, forward, up, right, yaw, pitch,
                new VisibilityProjectionMatrix(projection), width, height, revision,
                playerEyes, playerForward, playerYaw, playerPitch);
    }
}
