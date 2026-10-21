package io.github.kxyr0.echoitemattachments.forge.persistence;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.CustodyId;
import io.github.kxyr0.echoitemattachments.api.identity.ReleaseId;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;
import io.github.kxyr0.echoitemattachments.core.model.CustodyRecord;
import io.github.kxyr0.echoitemattachments.core.model.ReleasePlan;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;

final class AttachmentRecordNbt {
    CompoundTag write(CustodyRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("attachmentId", record.attachmentId().value());
        tag.putString("custodyId", record.custodyId().value());
        tag.putString("subjectKind", record.subject().kind().name());
        tag.putString("subject", record.subject().value());
        tag.putString("point", record.point().value());
        tag.putString("itemId", record.itemId());
        tag.putInt("count", record.count());
        tag.putInt("sourceCountBefore", record.sourceCountBefore());
        tag.putByteArray("stack", record.stackTag());
        tag.putString("state", record.state().name());
        tag.putLong("revision", record.revision());
        record.sourceEntityId().ifPresent(value -> tag.putString("sourceEntity", value));
        record.releaseId().ifPresent(value -> tag.putString("releaseId", value.value()));
        record.releasePlan().ifPresent(plan -> tag.put("releasePlan", writePlan(plan)));
        return tag;
    }

    CustodyRecord read(CompoundTag tag) {
        return new CustodyRecord(
                new AttachmentId(tag.getString("attachmentId")),
                new CustodyId(tag.getString("custodyId")),
                new AttachmentSubjectRef(SubjectKind.valueOf(tag.getString("subjectKind")),
                        tag.getString("subject")),
                new AttachmentPointId(tag.getString("point")),
                tag.getString("itemId"),
                tag.getInt("count"),
                tag.contains("sourceCountBefore") ? tag.getInt("sourceCountBefore") : tag.getInt("count"),
                tag.getByteArray("stack"),
                ItemCustodyState.valueOf(tag.getString("state")),
                tag.getLong("revision"),
                tag.contains("sourceEntity") ? Optional.of(tag.getString("sourceEntity")) : Optional.empty(),
                tag.contains("releaseId")
                        ? Optional.of(new ReleaseId(tag.getString("releaseId"))) : Optional.empty(),
                tag.contains("releasePlan")
                        ? Optional.of(readPlan(tag.getCompound("releasePlan"))) : Optional.empty());
    }

    private CompoundTag writePlan(ReleasePlan plan) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", plan.dimensionId());
        tag.putDouble("x", plan.x()); tag.putDouble("y", plan.y()); tag.putDouble("z", plan.z());
        tag.putDouble("vx", plan.velocityX()); tag.putDouble("vy", plan.velocityY());
        tag.putDouble("vz", plan.velocityZ());
        return tag;
    }

    private ReleasePlan readPlan(CompoundTag tag) {
        return new ReleasePlan(tag.getString("dimension"), tag.getDouble("x"), tag.getDouble("y"),
                tag.getDouble("z"), tag.getDouble("vx"), tag.getDouble("vy"), tag.getDouble("vz"));
    }
}
