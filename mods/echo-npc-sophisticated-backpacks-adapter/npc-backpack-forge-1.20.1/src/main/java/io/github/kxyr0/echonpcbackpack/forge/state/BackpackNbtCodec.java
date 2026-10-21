package io.github.kxyr0.echonpcbackpack.forge.state;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackAttachmentId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackReservationId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackTransitionId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.api.world.DimensionId;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class BackpackNbtCodec {
    public CompoundTag write(BackpackActorRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("ActorId", record.actorId.value());
        tag.putString("AttachmentId", record.attachmentId.value());
        if (!record.payload.isEmpty()) tag.put("Payload", record.payload.save(new CompoundTag()));
        tag.putString("AttachmentPoint", record.attachmentPoint.name());
        tag.putString("Status", record.status.name());
        optional(tag, "TransitionId", record.transitionId.map(NpcBackpackTransitionId::value));
        optional(tag, "CommandId", record.commandId.map(NpcBackpackCommandId::value));
        optional(tag, "ReservationId", record.reservationId.map(NpcBackpackReservationId::value));
        optional(tag, "TransitionType", record.transitionType.map(Enum::name));
        optional(tag, "Phase", record.phase.map(Enum::name));
        tag.putString("Policy", record.policy.name());
        record.source.ifPresent(value -> tag.put("Source", location(value)));
        record.destination.ifPresent(value -> tag.put("Destination", location(value)));
        optional(tag, "ExpectedFingerprint", record.expectedFingerprint);
        optional(tag, "NavigationMoveId", record.navigationMoveId);
        ListTag receipts = new ListTag();
        record.markerReceipts.forEach(value -> receipts.add(StringTag.valueOf(value)));
        tag.put("MarkerReceipts", receipts);
        tag.putString("DiagnosticCode", record.diagnosticCode);
        tag.putString("DiagnosticMessage", record.diagnosticMessage);
        tag.putLong("Revision", record.revision);
        return tag;
    }

    public BackpackActorRecord read(CompoundTag tag) {
        BackpackActorRecord record = new BackpackActorRecord();
        record.actorId = new ActorId(tag.getString("ActorId"));
        record.attachmentId = new NpcBackpackAttachmentId(tag.getString("AttachmentId"));
        record.payload = tag.contains("Payload", Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound("Payload")) : ItemStack.EMPTY;
        record.attachmentPoint = value(tag, "AttachmentPoint", NpcBackpackAttachmentPoint.class,
                NpcBackpackAttachmentPoint.NONE);
        record.status = value(tag, "Status", NpcBackpackStatus.class, NpcBackpackStatus.NONE);
        record.transitionId = string(tag, "TransitionId").map(NpcBackpackTransitionId::new);
        record.commandId = string(tag, "CommandId").map(NpcBackpackCommandId::new);
        record.reservationId = string(tag, "ReservationId").map(NpcBackpackReservationId::new);
        record.transitionType = string(tag, "TransitionType").map(NpcBackpackTransitionType::valueOf);
        record.phase = string(tag, "Phase").map(NpcBackpackTransitionPhase::valueOf);
        record.policy = value(tag, "Policy", NpcBackpackTransitionPolicy.class,
                NpcBackpackTransitionPolicy.IMMEDIATE);
        if (tag.contains("Source", Tag.TAG_COMPOUND)) record.source = Optional.of(location(tag.getCompound("Source")));
        if (tag.contains("Destination", Tag.TAG_COMPOUND)) {
            record.destination = Optional.of(location(tag.getCompound("Destination")));
        }
        record.expectedFingerprint = string(tag, "ExpectedFingerprint");
        record.navigationMoveId = string(tag, "NavigationMoveId");
        tag.getList("MarkerReceipts", Tag.TAG_STRING).forEach(item -> record.markerReceipts.add(item.getAsString()));
        record.diagnosticCode = tag.getString("DiagnosticCode");
        record.diagnosticMessage = tag.getString("DiagnosticMessage");
        record.revision = Math.max(1L, tag.getLong("Revision"));
        return record;
    }

    private CompoundTag location(BackpackBlockLocation value) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", value.dimension().value());
        tag.putInt("X", value.position().x());
        tag.putInt("Y", value.position().y());
        tag.putInt("Z", value.position().z());
        return tag;
    }

    private BackpackBlockLocation location(CompoundTag tag) {
        return new BackpackBlockLocation(new DimensionId(tag.getString("Dimension")),
                new BlockPosition(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")));
    }

    private void optional(CompoundTag tag, String key, Optional<String> value) {
        value.ifPresent(text -> tag.putString(key, text));
    }

    private Optional<String> string(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    private <E extends Enum<E>> E value(CompoundTag tag, String key, Class<E> type, E fallback) {
        try {
            return Enum.valueOf(type, tag.getString(key));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
