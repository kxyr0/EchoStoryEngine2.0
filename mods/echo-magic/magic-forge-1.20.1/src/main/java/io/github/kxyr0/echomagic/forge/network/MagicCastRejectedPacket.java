package io.github.kxyr0.echomagic.forge.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

public record MagicCastRejectedPacket(String reason) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeUtf(reason); }
    public static MagicCastRejectedPacket decode(FriendlyByteBuf buffer) { return new MagicCastRejectedPacket(buffer.readUtf(256)); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new MagicClientPacketEvent.Rejected(this)));
        context.setPacketHandled(true);
    }
}
