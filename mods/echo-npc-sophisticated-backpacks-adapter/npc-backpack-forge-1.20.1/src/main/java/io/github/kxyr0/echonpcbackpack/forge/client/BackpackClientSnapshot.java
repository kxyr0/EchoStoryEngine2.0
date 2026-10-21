package io.github.kxyr0.echonpcbackpack.forge.client;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import net.minecraft.world.item.ItemStack;

public record BackpackClientSnapshot(ActorId actorId, ItemStack payload,
        NpcBackpackAttachmentPoint attachmentPoint, NpcBackpackStatus status, long revision) {
    public BackpackClientSnapshot {
        payload = payload.copy();
    }

    @Override
    public ItemStack payload() {
        return payload.copy();
    }
}
