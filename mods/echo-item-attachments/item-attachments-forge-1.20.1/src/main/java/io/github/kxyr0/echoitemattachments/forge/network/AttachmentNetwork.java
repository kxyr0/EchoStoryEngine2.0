package io.github.kxyr0.echoitemattachments.forge.network;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.core.model.CustodyRecord;
import io.github.kxyr0.echoitemattachments.forge.persistence.ItemStackCodec;
import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AttachmentNetwork {
    private static final String VERSION = "1";
    private final ItemStackCodec stacks = new ItemStackCodec();
    private final SimpleChannel channel = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("echo_item_attachments", "sync"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public void register() {
        channel.messageBuilder(AttachmentSnapshotPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AttachmentSnapshotPayload::encode)
                .decoder(AttachmentSnapshotPayload::decode)
                .consumerMainThread(AttachmentSnapshotPayload::handle).add();
    }

    public void sendSubject(MinecraftServer server, AttachmentSubjectRef subject,
            Collection<CustodyRecord> records) {
        AttachmentSnapshotPayload payload = payload(subject, records);
        server.getPlayerList().getPlayers().forEach(player -> channel.send(
                PacketDistributor.PLAYER.with(() -> player), payload));
    }

    public void sendAll(ServerPlayer player, Collection<CustodyRecord> records) {
        records.stream().map(CustodyRecord::subject).distinct().forEach(subject -> channel.send(
                PacketDistributor.PLAYER.with(() -> player), payload(subject, records)));
    }

    private AttachmentSnapshotPayload payload(AttachmentSubjectRef subject,
            Collection<CustodyRecord> records) {
        List<CustodyRecord> visible = records.stream().filter(record -> record.subject().equals(subject)
                && (record.state() == io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState.ATTACHED_TO_SUBJECT
                || record.state() == io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState.RELEASING)).toList();
        long revision = records.stream().filter(record -> record.subject().equals(subject))
                .mapToLong(CustodyRecord::revision).max().orElse(0L);
        List<AttachmentSnapshotPayload.Entry> entries = visible.stream().map(record ->
                new AttachmentSnapshotPayload.Entry(record.attachmentId(), record.point(),
                        stacks.decode(record.stackTag()), record.revision())).toList();
        return new AttachmentSnapshotPayload(subject, revision, entries);
    }
}
