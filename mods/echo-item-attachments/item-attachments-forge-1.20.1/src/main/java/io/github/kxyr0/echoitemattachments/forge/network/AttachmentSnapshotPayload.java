package io.github.kxyr0.echoitemattachments.forge.network;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record AttachmentSnapshotPayload(
        AttachmentSubjectRef subject,
        long revision,
        List<Entry> entries) {
    public AttachmentSnapshotPayload {
        entries = List.copyOf(entries);
    }

    public static void encode(AttachmentSnapshotPayload payload, FriendlyByteBuf buffer) {
        buffer.writeEnum(payload.subject.kind());
        buffer.writeUtf(payload.subject.value(), 128);
        buffer.writeVarLong(payload.revision);
        buffer.writeVarInt(payload.entries.size());
        payload.entries.forEach(entry -> {
            buffer.writeUtf(entry.attachmentId.value(), 128);
            buffer.writeUtf(entry.point.value(), 96);
            buffer.writeItem(entry.stack);
            buffer.writeVarLong(entry.revision);
        });
    }

    public static AttachmentSnapshotPayload decode(FriendlyByteBuf buffer) {
        AttachmentSubjectRef subject = new AttachmentSubjectRef(buffer.readEnum(SubjectKind.class),
                buffer.readUtf(128));
        long revision = buffer.readVarLong();
        int size = buffer.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new Entry(new AttachmentId(buffer.readUtf(128)),
                    new AttachmentPointId(buffer.readUtf(96)), buffer.readItem(), buffer.readVarLong()));
        }
        return new AttachmentSnapshotPayload(subject, revision, entries);
    }

    public static void handle(AttachmentSnapshotPayload payload,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientAttachmentStore.accept(payload));
        context.setPacketHandled(true);
    }

    public record Entry(AttachmentId attachmentId, AttachmentPointId point,
            ItemStack stack, long revision) {
        public Entry {
            stack = stack.copy();
        }
    }
}
