package io.github.kxyr0.echostoryengine.blackspace.network;

import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import io.github.kxyr0.echostoryengine.blackspace.client.BlackspaceClientState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record SetBlackspaceStatePacket(boolean active) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
    }

    public static SetBlackspaceStatePacket decode(FriendlyByteBuf buffer) {
        return new SetBlackspaceStatePacket(buffer.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> applyClientState(active)));
        context.setPacketHandled(true);
    }

    private static void applyClientState(boolean active) {
        try {
            BlackspaceClientState.setActive(active);
        } catch (RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Echo Blackspace failed to apply client state {}.", active, exception);
        }
    }
}
