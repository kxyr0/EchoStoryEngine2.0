package io.github.kxyr0.echocutscene.forge.network.authoring;

import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundAuthoringModePacket(String sessionId, CameraControlMode mode) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId);
        buffer.writeEnum(mode);
    }

    public static ClientboundAuthoringModePacket decode(FriendlyByteBuf buffer) {
        return new ClientboundAuthoringModePacket(buffer.readUtf(), buffer.readEnum(CameraControlMode.class));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoCutsceneClient.setAuthoringMode(this)));
        context.setPacketHandled(true);
    }
}
