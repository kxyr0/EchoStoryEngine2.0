package io.github.kxyr0.echocutscene.forge.network.codec;

import io.github.kxyr0.echocutscene.api.camera.framing.CameraFraming;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetAnchor;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetBinding;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetId;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKind;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import net.minecraft.network.FriendlyByteBuf;

public final class CameraTargetBuf {
    private CameraTargetBuf() {
    }

    public static void writeTarget(FriendlyByteBuf buffer, CameraTarget target) {
        buffer.writeEnum(target.kind());
        buffer.writeEnum(target.binding());
        buffer.writeEnum(target.anchor());
        buffer.writeUtf(target.primaryId().value());
        buffer.writeUtf(target.secondaryId().value());
        writeVector(buffer, target.position());
        writeVector(buffer, target.offset());
        buffer.writeDouble(target.weight());
    }

    public static CameraTarget readTarget(FriendlyByteBuf buffer) {
        CameraTargetKind kind = buffer.readEnum(CameraTargetKind.class);
        CameraTargetBinding binding = buffer.readEnum(CameraTargetBinding.class);
        CameraTargetAnchor anchor = buffer.readEnum(CameraTargetAnchor.class);
        CameraTargetId primaryId = new CameraTargetId(buffer.readUtf());
        CameraTargetId secondaryId = new CameraTargetId(buffer.readUtf());
        Vector3 position = readVector(buffer);
        Vector3 offset = readVector(buffer);
        return new CameraTarget(kind, binding, anchor, primaryId, secondaryId, position, offset,
                buffer.readDouble());
    }

    public static void writeFraming(FriendlyByteBuf buffer, CameraFraming framing) {
        buffer.writeDouble(framing.horizontal());
        buffer.writeDouble(framing.vertical());
    }

    public static CameraFraming readFraming(FriendlyByteBuf buffer) {
        return new CameraFraming(buffer.readDouble(), buffer.readDouble());
    }

    private static void writeVector(FriendlyByteBuf buffer, Vector3 value) {
        buffer.writeDouble(value.x());
        buffer.writeDouble(value.y());
        buffer.writeDouble(value.z());
    }

    private static Vector3 readVector(FriendlyByteBuf buffer) {
        return new Vector3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
