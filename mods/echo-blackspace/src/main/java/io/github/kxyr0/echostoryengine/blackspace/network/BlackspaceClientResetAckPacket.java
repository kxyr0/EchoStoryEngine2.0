package io.github.kxyr0.echostoryengine.blackspace.network;

import io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceScene;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record BlackspaceClientResetAckPacket(long resetId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(resetId);
    }

    public static BlackspaceClientResetAckPacket decode(FriendlyByteBuf buffer) {
        return new BlackspaceClientResetAckPacket(buffer.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> complete(context.getSender()));
        context.setPacketHandled(true);
    }

    private void complete(ServerPlayer player) {
        if (player != null) {
            BlackspaceScene.completeClientReset(player, resetId);
        }
    }
}
