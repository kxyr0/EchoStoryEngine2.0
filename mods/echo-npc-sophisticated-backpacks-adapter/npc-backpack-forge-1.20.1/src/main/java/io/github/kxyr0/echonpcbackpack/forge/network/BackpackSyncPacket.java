package io.github.kxyr0.echonpcbackpack.forge.network;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.forge.client.BackpackClientState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record BackpackSyncPacket(ActorId actorId, ItemStack payload,
        NpcBackpackAttachmentPoint attachmentPoint, NpcBackpackStatus status, long revision) {
    public BackpackSyncPacket {
        payload = payload.copy();
    }

    static void encode(BackpackSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.actorId.value(), 128);
        buffer.writeItem(packet.payload);
        buffer.writeEnum(packet.attachmentPoint);
        buffer.writeEnum(packet.status);
        buffer.writeVarLong(packet.revision);
    }

    static BackpackSyncPacket decode(FriendlyByteBuf buffer) {
        return new BackpackSyncPacket(new ActorId(buffer.readUtf(128)), buffer.readItem(),
                buffer.readEnum(NpcBackpackAttachmentPoint.class), buffer.readEnum(NpcBackpackStatus.class),
                buffer.readVarLong());
    }

    static void handle(BackpackSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> BackpackClientState.apply(packet));
        context.get().setPacketHandled(true);
    }
}
