package io.github.kxyr0.echoitemattachments.forge.runtime;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;
import io.github.kxyr0.echoitemattachments.forge.persistence.ItemStackCodec;
import io.github.kxyr0.echoitemattachments.forge.runtime.release.ReleaseSpawner;
import io.github.kxyr0.echoitemattachments.forge.runtime.release.ReleaseWitness;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;

final class AttachmentRecovery {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ServerAttachmentHost host;
    private final ReleaseWitness witnesses = new ReleaseWitness();
    private final ReleaseSpawner spawner = new ReleaseSpawner();
    private final ItemStackCodec stacks = new ItemStackCodec();

    AttachmentRecovery(ServerAttachmentHost host) {
        this.host = host;
    }

    void recover() {
        host.attachments().records().forEach(record -> {
            try {
                recover(record);
            } catch (RuntimeException exception) {
                LOGGER.error("event=item_attachment_recovery_failed attachment={} custody={} state={} code=EIA-RECOVERY",
                        record.attachmentId(), record.custodyId(), record.state(), exception);
            }
        });
        host.persist();
    }

    private void recover(io.github.kxyr0.echoitemattachments.core.model.CustodyRecord record) {
        if (record.state() == ItemCustodyState.WORLD_ITEM) {
            recoverReservedWorld(record);
        } else if (record.state() == ItemCustodyState.ATTACHED_TO_SUBJECT) {
            host.sync(record.subject());
        } else if (record.state() == ItemCustodyState.RELEASING) {
            recoverRelease(record);
        } else if (record.state() == ItemCustodyState.WORLD_RELEASED) {
            finishReleased(record);
        } else if (record.state() == ItemCustodyState.RECOVERING) {
            recoverAttach(record);
        } else {
            LOGGER.error("event=item_attachment_quarantined attachment={} code=EIA-RECOVERY-STATE",
                    record.attachmentId());
        }
    }

    private void recoverReservedWorld(
            io.github.kxyr0.echoitemattachments.core.model.CustodyRecord record) {
        if (record.sourceEntityId().flatMap(this::source).isPresent()) {
            host.attachments().abandonWorld(record.attachmentId());
            LOGGER.info("event=item_attachment_recovered action=keep_source attachment={}", record.attachmentId());
            return;
        }
        host.attachments().abandonWorld(record.attachmentId());
        LOGGER.warn("event=item_attachment_recovered action=source_changed attachment={} code=EIA-SOURCE-CHANGED",
                record.attachmentId());
    }

    private void recoverAttach(io.github.kxyr0.echoitemattachments.core.model.CustodyRecord record) {
        var source = record.sourceEntityId().flatMap(this::source);
        if (source.isEmpty()) {
            restoreHand(record, "source_removed");
            return;
        }
        var live = source.orElseThrow().getItem();
        var payload = stacks.decode(record.stackTag());
        if (!net.minecraft.world.item.ItemStack.isSameItemSameTags(live, payload)) {
            quarantine(record, "EIA-RECOVERY-SOURCE-PAYLOAD");
            return;
        }
        if (live.getCount() == record.sourceCountBefore()) {
            host.attachments().abandonRecovery(record.attachmentId());
            LOGGER.info("event=item_attachment_recovered action=rollback_attach attachment={}", record.attachmentId());
        } else if (live.getCount() == record.sourceCountBefore() - record.count()) {
            restoreHand(record, "source_split");
        } else {
            quarantine(record, "EIA-RECOVERY-SOURCE-COUNT");
        }
    }

    private void restoreHand(io.github.kxyr0.echoitemattachments.core.model.CustodyRecord record, String witness) {
        host.attachments().completeAttach(record.attachmentId());
        host.sync(record.subject());
        LOGGER.warn("event=item_attachment_recovered action=restore_hand witness={} attachment={}",
                witness, record.attachmentId());
    }

    private void quarantine(io.github.kxyr0.echoitemattachments.core.model.CustodyRecord record, String code) {
        LOGGER.error("event=item_attachment_quarantined attachment={} code={}", record.attachmentId(), code);
    }

    private void recoverRelease(io.github.kxyr0.echoitemattachments.core.model.CustodyRecord record) {
        var releaseId = record.releaseId().orElseThrow();
        if (witnesses.find(host.server(), releaseId).isEmpty()) {
            var plan = record.releasePlan().orElseThrow();
            ResourceKey<net.minecraft.world.level.Level> key = ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation(plan.dimensionId()));
            ServerLevel level = host.server().getLevel(key);
            if (level == null) throw new IllegalStateException("release dimension is unavailable");
            spawner.spawnPrepared(level, stacks.decode(record.stackTag()), releaseId, plan);
        }
        host.attachments().confirmReleased(record.attachmentId());
        host.sync(record.subject());
        host.attachments().forgetReleased(record.attachmentId());
        LOGGER.info("event=item_attachment_recovered action=confirm_release attachment={} release={}",
                record.attachmentId(), releaseId);
    }

    private void finishReleased(io.github.kxyr0.echoitemattachments.core.model.CustodyRecord record) {
        boolean present = record.releaseId().flatMap(id -> witnesses.find(host.server(), id)).isPresent();
        host.sync(record.subject());
        host.attachments().forgetReleased(record.attachmentId());
        LOGGER.info("event=item_attachment_recovered action=finish_released witness_present={} attachment={}",
                present, record.attachmentId());
    }

    private java.util.Optional<ItemEntity> source(String uuid) {
        UUID id = UUID.fromString(uuid);
        for (ServerLevel level : host.server().getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof ItemEntity item && item.isAlive()) return java.util.Optional.of(item);
        }
        return java.util.Optional.empty();
    }
}
