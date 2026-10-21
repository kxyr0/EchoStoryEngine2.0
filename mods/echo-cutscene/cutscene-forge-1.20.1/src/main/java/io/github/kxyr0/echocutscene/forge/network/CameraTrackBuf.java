package io.github.kxyr0.echocutscene.forge.network;

import io.github.kxyr0.echocutscene.api.camera.CameraFieldOfView;
import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraPosition;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackMetadata;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.camera.curve.CameraInterpolation;
import io.github.kxyr0.echocutscene.api.camera.framing.CameraFramingKeyframe;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraModeKeyframe;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetKeyframe;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackDataId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackRevision;
import io.github.kxyr0.echocutscene.forge.network.codec.CameraTargetBuf;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

final class CameraTrackBuf {
    private CameraTrackBuf() {
    }

    static void write(FriendlyByteBuf buffer, CameraTrackSnapshot snapshot) {
        writeMetadata(buffer, snapshot.metadata());
        buffer.writeVarInt(snapshot.keyframes().size());
        snapshot.keyframes().forEach(frame -> writeFrame(buffer, frame));
        writeModes(buffer, snapshot.modeKeyframes());
        writeTargets(buffer, snapshot.targetKeyframes());
        writeFramings(buffer, snapshot.framingKeyframes());
    }

    static CameraTrackSnapshot read(FriendlyByteBuf buffer) {
        CameraTrackMetadata metadata = readMetadata(buffer);
        int count = buffer.readVarInt();
        List<CameraKeyframe> frames = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            frames.add(readFrame(buffer));
        }
        return new CameraTrackSnapshot(metadata, frames, readModes(buffer), readTargets(buffer),
                readFramings(buffer));
    }

    private static void writeModes(FriendlyByteBuf buffer, List<CameraModeKeyframe> frames) {
        buffer.writeVarInt(frames.size());
        for (CameraModeKeyframe frame : frames) {
            buffer.writeLong(frame.timeMillis());
            buffer.writeEnum(frame.mode());
        }
    }

    private static List<CameraModeKeyframe> readModes(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<CameraModeKeyframe> frames = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            frames.add(new CameraModeKeyframe(buffer.readLong(), buffer.readEnum(CameraControlMode.class)));
        }
        return frames;
    }

    private static void writeTargets(FriendlyByteBuf buffer, List<CameraTargetKeyframe> frames) {
        buffer.writeVarInt(frames.size());
        for (CameraTargetKeyframe frame : frames) {
            buffer.writeLong(frame.timeMillis());
            CameraTargetBuf.writeTarget(buffer, frame.target());
        }
    }

    private static List<CameraTargetKeyframe> readTargets(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<CameraTargetKeyframe> frames = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            frames.add(new CameraTargetKeyframe(buffer.readLong(), CameraTargetBuf.readTarget(buffer)));
        }
        return frames;
    }

    private static void writeFramings(FriendlyByteBuf buffer, List<CameraFramingKeyframe> frames) {
        buffer.writeVarInt(frames.size());
        for (CameraFramingKeyframe frame : frames) {
            buffer.writeLong(frame.timeMillis());
            CameraTargetBuf.writeFraming(buffer, frame.framing());
        }
    }

    private static List<CameraFramingKeyframe> readFramings(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<CameraFramingKeyframe> frames = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            frames.add(new CameraFramingKeyframe(buffer.readLong(), CameraTargetBuf.readFraming(buffer)));
        }
        return frames;
    }

    private static void writeMetadata(FriendlyByteBuf buffer, CameraTrackMetadata metadata) {
        buffer.writeUtf(metadata.trackId().value());
        buffer.writeLong(metadata.revision().value());
        buffer.writeUtf(metadata.dataId().value());
        buffer.writeLong(metadata.durationMillis());
        buffer.writeVarInt(metadata.sampleRate());
        buffer.writeVarInt(metadata.schemaVersion());
        buffer.writeUtf(metadata.fingerprint());
    }

    private static CameraTrackMetadata readMetadata(FriendlyByteBuf buffer) {
        return new CameraTrackMetadata(CameraTrackId.parse(buffer.readUtf()),
                new CameraTrackRevision(buffer.readLong()), new CameraTrackDataId(buffer.readUtf()),
                buffer.readLong(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf());
    }

    private static void writeFrame(FriendlyByteBuf buffer, CameraKeyframe frame) {
        Vector3 position = frame.pose().position().value();
        CameraRotation rotation = frame.pose().rotation();
        buffer.writeLong(frame.timeMillis());
        buffer.writeDouble(position.x());
        buffer.writeDouble(position.y());
        buffer.writeDouble(position.z());
        buffer.writeDouble(rotation.yaw());
        buffer.writeDouble(rotation.pitch());
        buffer.writeDouble(rotation.roll());
        buffer.writeDouble(frame.pose().fieldOfView().degrees());
        buffer.writeEnum(frame.interpolation());
    }

    private static CameraKeyframe readFrame(FriendlyByteBuf buffer) {
        long time = buffer.readLong();
        CameraPosition position = new CameraPosition(new Vector3(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble()));
        CameraRotation rotation = new CameraRotation(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        CameraFieldOfView fieldOfView = new CameraFieldOfView(buffer.readDouble());
        return new CameraKeyframe(time, new CameraPose(position, rotation, fieldOfView),
                buffer.readEnum(CameraInterpolation.class));
    }
}
