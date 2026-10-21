package io.github.kxyr0.echomagic.forge.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

public record MagicImpactPacket(double x, double y, double z, String style, float red, float green, float blue,
                                float vanillaRed, float vanillaGreen, float vanillaBlue) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(x); buffer.writeDouble(y); buffer.writeDouble(z); buffer.writeUtf(style);
        buffer.writeFloat(red); buffer.writeFloat(green); buffer.writeFloat(blue);
        buffer.writeFloat(vanillaRed); buffer.writeFloat(vanillaGreen); buffer.writeFloat(vanillaBlue);
    }
    public static MagicImpactPacket decode(FriendlyByteBuf buffer) {
        return new MagicImpactPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readUtf(256), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new MagicClientPacketEvent.Impact(this)));
        context.setPacketHandled(true);
    }
}
