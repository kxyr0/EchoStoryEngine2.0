package io.github.kxyr0.echonpcbackpack.forge.network;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.forge.client.BackpackDebugState;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record BackpackDebugPacket(Optional<ActorId> actorId) {
    static void encode(BackpackDebugPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.actorId.isPresent());
        packet.actorId.ifPresent(actor -> buffer.writeUtf(actor.value(), 128));
    }

    static BackpackDebugPacket decode(FriendlyByteBuf buffer) {
        return new BackpackDebugPacket(buffer.readBoolean()
                ? Optional.of(new ActorId(buffer.readUtf(128))) : Optional.empty());
    }

    static void handle(BackpackDebugPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> BackpackDebugState.set(packet.actorId));
        context.get().setPacketHandled(true);
    }
}
