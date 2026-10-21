package io.github.kxyr0.echomagic.forge.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record MagicCastAcceptedPacket(String castId, String spellId, long acceptedTick) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeUtf(castId); buffer.writeUtf(spellId); buffer.writeVarLong(acceptedTick); }
    public static MagicCastAcceptedPacket decode(FriendlyByteBuf buffer) {
        return new MagicCastAcceptedPacket(buffer.readUtf(128), buffer.readUtf(256), buffer.readVarLong());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.setPacketHandled(true);
    }
}

