package io.github.kxyr0.echomagic.forge.network.gesture;

import io.github.kxyr0.echomagic.forge.EchoMagicMod;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record MagicDrawingStatePacket(boolean active) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(active); }
    public static MagicDrawingStatePacket decode(FriendlyByteBuf buffer) {
        return new MagicDrawingStatePacket(buffer.readBoolean());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> update(context.getSender()));
        context.setPacketHandled(true);
    }
    private void update(ServerPlayer player) {
        if (player != null) EchoMagicMod.drawingState(player,
                active && io.github.kxyr0.echomagic.forge.service.MagicFeatureAccess.server(player.getUUID()));
    }
}
