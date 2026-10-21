package io.github.kxyr0.echomagic.forge.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

public record MagicPlayerStateSnapshotPacket(List<String> known, Optional<String> selected,
        Optional<String> gestureSchool, long cooldownRemaining, long revision) {
    public MagicPlayerStateSnapshotPacket { known = List.copyOf(known); }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(known.size());
        known.forEach(buffer::writeUtf);
        buffer.writeOptional(selected, FriendlyByteBuf::writeUtf);
        buffer.writeOptional(gestureSchool, FriendlyByteBuf::writeUtf);
        buffer.writeVarLong(cooldownRemaining);
        buffer.writeVarLong(revision);
    }
    public static MagicPlayerStateSnapshotPacket decode(FriendlyByteBuf buffer) {
        int size = Math.min(1024, buffer.readVarInt());
        List<String> known = new ArrayList<>(size);
        for (int index = 0; index < size; index++) known.add(buffer.readUtf(256));
        return new MagicPlayerStateSnapshotPacket(known, buffer.readOptional(value -> value.readUtf(256)),
                buffer.readOptional(value -> value.readUtf(32)), buffer.readVarLong(), buffer.readVarLong());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new MagicClientPacketEvent.State(this)));
        context.setPacketHandled(true);
    }
}
