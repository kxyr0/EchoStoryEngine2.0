package io.github.kxyr0.echostoryengine.blackspace.network;

import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import io.github.kxyr0.echostoryengine.blackspace.shader.BlackspaceShaderController;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PrepareBlackspaceEnterPacket(long prepareId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(prepareId);
    }

    public static PrepareBlackspaceEnterPacket decode(FriendlyByteBuf buffer) {
        return new PrepareBlackspaceEnterPacket(buffer.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> prepareClientShader(prepareId)));
        context.setPacketHandled(true);
    }

    private static void prepareClientShader(long prepareId) {
        try {
            BlackspaceShaderController.prepareEnter(prepareId);
        } catch (RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Echo Blackspace failed to prepare client shader.", exception);
            BlackspaceNetwork.sendShaderReady(prepareId);
        }
    }
}
