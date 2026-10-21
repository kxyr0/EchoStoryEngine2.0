package io.github.kxyr0.echocutscene.forge.network.authoring;

import io.github.kxyr0.echocutscene.api.camera.CameraFieldOfView;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraPosition;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundAuthoringStartPacket(
        String sessionId,
        CutsceneId cutsceneId,
        CameraPose initialPose,
        double cameraSpeed
) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId);
        buffer.writeUtf(cutsceneId.value());
        writePose(buffer, initialPose);
        buffer.writeDouble(cameraSpeed);
    }

    public static ClientboundAuthoringStartPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundAuthoringStartPacket(buffer.readUtf(), CutsceneId.parse(buffer.readUtf()),
                readPose(buffer), buffer.readDouble());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoCutsceneClient.beginAuthoring(this)));
        context.setPacketHandled(true);
    }

    private static void writePose(FriendlyByteBuf buffer, CameraPose pose) {
        Vector3 position = pose.position().value();
        buffer.writeDouble(position.x());
        buffer.writeDouble(position.y());
        buffer.writeDouble(position.z());
        buffer.writeDouble(pose.rotation().yaw());
        buffer.writeDouble(pose.rotation().pitch());
        buffer.writeDouble(pose.fieldOfView().degrees());
    }

    private static CameraPose readPose(FriendlyByteBuf buffer) {
        CameraPosition position = new CameraPosition(new Vector3(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble()));
        CameraRotation rotation = new CameraRotation(buffer.readDouble(), buffer.readDouble(), 0.0D);
        return new CameraPose(position, rotation, new CameraFieldOfView(buffer.readDouble()));
    }
}
