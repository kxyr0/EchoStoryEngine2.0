package io.github.kxyr0.echocutscene.forge.network.authoring;

import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient;
import io.github.kxyr0.echocutscene.forge.network.codec.CameraTargetBuf;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundAuthoringTargetPacket(String sessionId, Optional<CameraTarget> target) {
    public ClientboundAuthoringTargetPacket {
        target = java.util.Objects.requireNonNull(target, "target");
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId);
        buffer.writeBoolean(target.isPresent());
        target.ifPresent(value -> CameraTargetBuf.writeTarget(buffer, value));
    }

    public static ClientboundAuthoringTargetPacket decode(FriendlyByteBuf buffer) {
        String sessionId = buffer.readUtf();
        Optional<CameraTarget> target = buffer.readBoolean()
                ? Optional.of(CameraTargetBuf.readTarget(buffer)) : Optional.empty();
        return new ClientboundAuthoringTargetPacket(sessionId, target);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoCutsceneClient.setAuthoringTarget(this)));
        context.setPacketHandled(true);
    }
}
