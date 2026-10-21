package io.github.kxyr0.echodamagenumbers.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

public record DamagePopupPacket(double x, double y, double z, float amount) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeFloat(amount);
    }

    public static DamagePopupPacket decode(FriendlyByteBuf buffer) {
        return new DamagePopupPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new DamagePopupReceivedEvent(this)));
        context.setPacketHandled(true);
    }
}

