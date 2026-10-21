package io.github.kxyr0.echoanimation.forge.playback.nbt;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileFingerprint;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectLifecycleOwnerId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvenance;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * NBT codec for durable animation subject records.
 */
public final class AnimationSubjectNbtCodec {
    public CompoundTag subjectTag(SubjectRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("SubjectId", record.subjectId.value());
        tag.putLong("Revision", record.revision);
        tag.putLong("Generation", record.generation);
        record.profileId.ifPresent(id -> tag.putString("ProfileId", id.value()));
        record.profileFingerprint.ifPresent(value -> tag.putString("ProfileFingerprint", value.value()));
        record.pendingProfileId.ifPresent(id -> tag.putString("PendingProfileId", id.value()));
        record.pendingProfileFingerprint.ifPresent(value -> tag.putString("PendingProfileFingerprint", value.value()));
        record.provenance.ifPresent(provenance -> provenanceTag(tag, provenance));
        tag.put("Channels", channelTags(record));
        return tag;
    }

    public SubjectRecord subject(CompoundTag tag) {
        SubjectRecord record = new SubjectRecord(new AnimationSubjectId(tag.getString("SubjectId")));
        record.revision = tag.getLong("Revision");
        record.generation = Math.max(1L, tag.getLong("Generation"));
        record.profileId = optionalString(tag, "ProfileId").map(AnimationProfileId::parse);
        record.profileFingerprint = optionalString(tag, "ProfileFingerprint").map(AnimationProfileFingerprint::new);
        record.pendingProfileId = optionalString(tag, "PendingProfileId").map(AnimationProfileId::parse);
        record.pendingProfileFingerprint = optionalString(tag, "PendingProfileFingerprint")
                .map(AnimationProfileFingerprint::new);
        if (tag.contains("Provenance", Tag.TAG_COMPOUND)) {
            record.provenance = Optional.of(provenance(tag.getCompound("Provenance")));
        }
        for (Tag item : tag.getList("Channels", Tag.TAG_COMPOUND)) {
            CompoundTag channel = (CompoundTag) item;
            record.channelStates.put(new AnimationChannelId(channel.getString("Channel")),
                    new AnimationStateId(channel.getString("State")));
        }
        return record;
    }

    private void provenanceTag(CompoundTag tag, AnimationSubjectProvenance provenance) {
        CompoundTag item = new CompoundTag();
        item.putString("LifecycleOwnerId", provenance.lifecycleOwnerId().value());
        item.putString("ExternalCanonicalKey", provenance.externalCanonicalKey());
        tag.put("Provenance", item);
    }

    private AnimationSubjectProvenance provenance(CompoundTag item) {
        return new AnimationSubjectProvenance(
                AnimationSubjectLifecycleOwnerId.parse(item.getString("LifecycleOwnerId")),
                item.getString("ExternalCanonicalKey"));
    }

    private ListTag channelTags(SubjectRecord record) {
        ListTag channels = new ListTag();
        record.channelStates.forEach((channel, state) -> {
            CompoundTag item = new CompoundTag();
            item.putString("Channel", channel.value());
            item.putString("State", state.value());
            channels.add(item);
        });
        return channels;
    }

    private Optional<String> optionalString(CompoundTag tag, String key) {
        return tag.contains(key) ? Optional.of(tag.getString(key)) : Optional.empty();
    }
}
