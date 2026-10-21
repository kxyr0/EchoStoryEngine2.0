package io.github.kxyr0.echostoryengine.blackspace.network;

import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import io.github.kxyr0.echostoryengine.blackspace.client.BlackspaceClientState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record BlackspaceSessionStatePacket(boolean active, String blackspaceId, long resetId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeUtf(blackspaceId);
        buffer.writeLong(resetId);
    }

    public static BlackspaceSessionStatePacket decode(FriendlyByteBuf buffer) {
        return new BlackspaceSessionStatePacket(buffer.readBoolean(), buffer.readUtf(), buffer.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> applyClientState(active, blackspaceId, resetId)));
        context.setPacketHandled(true);
    }

    private static void applyClientState(boolean active, String blackspaceId, long resetId) {
        try {
            BlackspaceClientState.applySessionState(active, blackspaceId, resetId);
        } catch (RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Echo Blackspace failed to apply client session state active={} id={}.",
                    active, blackspaceId, exception);
            if (!active && resetId != 0L) {
                BlackspaceNetwork.sendClientResetAck(resetId);
            }
        }
    }
}
