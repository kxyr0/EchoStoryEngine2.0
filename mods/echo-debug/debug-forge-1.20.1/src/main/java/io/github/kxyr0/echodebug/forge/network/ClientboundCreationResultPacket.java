package io.github.kxyr0.echodebug.forge.network;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.forge.client.EchoDebugClient;
import io.github.kxyr0.echodebug.forge.creation.TriggerCreationResult;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundCreationResultPacket(
        boolean successful,
        DebugDiagnosticCode code,
        String message,
        String triggerId,
        String targetPath
) {
    public static ClientboundCreationResultPacket from(TriggerCreationResult result) {
        return new ClientboundCreationResultPacket(result.successful(), result.code(), result.message(),
                result.triggerId().orElse(""), result.targetPath().map(Object::toString).orElse(""));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(successful);
        buffer.writeEnum(code);
        buffer.writeUtf(message);
        buffer.writeUtf(triggerId);
        buffer.writeUtf(targetPath);
    }

    public static ClientboundCreationResultPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundCreationResultPacket(buffer.readBoolean(),
                buffer.readEnum(DebugDiagnosticCode.class), buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoDebugClient.applyCreationResult(this)));
        context.setPacketHandled(true);
    }
}
