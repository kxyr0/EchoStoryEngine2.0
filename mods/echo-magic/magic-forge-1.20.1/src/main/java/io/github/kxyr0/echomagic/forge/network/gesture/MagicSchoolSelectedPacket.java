package io.github.kxyr0.echomagic.forge.network.gesture;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import io.github.kxyr0.echomagic.forge.network.MagicClientPacketEvent;

public record MagicSchoolSelectedPacket(String school) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeUtf(school); }
    public static MagicSchoolSelectedPacket decode(FriendlyByteBuf buffer) {
        return new MagicSchoolSelectedPacket(buffer.readUtf(32));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new MagicClientPacketEvent.SchoolSelected(this)));
        context.setPacketHandled(true);
    }
}
