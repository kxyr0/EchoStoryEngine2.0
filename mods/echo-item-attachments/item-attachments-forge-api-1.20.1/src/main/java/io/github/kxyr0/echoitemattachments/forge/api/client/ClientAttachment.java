package io.github.kxyr0.echoitemattachments.forge.api.client;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

public record ClientAttachment(
        AttachmentId attachmentId,
        AttachmentPointId point,
        ItemStack stack,
        long revision) {
    public ClientAttachment {
        attachmentId = Objects.requireNonNull(attachmentId, "attachment id");
        point = Objects.requireNonNull(point, "point");
        stack = Objects.requireNonNull(stack, "stack").copy();
        if (stack.isEmpty()) throw new IllegalArgumentException("stack must be non-empty");
        if (revision < 1) throw new IllegalArgumentException("revision must be positive");
    }

    @Override
    public ItemStack stack() {
        return stack.copy();
    }
}
