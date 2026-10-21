package io.github.kxyr0.echoanimation.forge.playback;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerId;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationLeaseId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.ArchivedDefinition;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.CommandRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.TerminalRecord;
import io.github.kxyr0.echoanimation.forge.playback.nbt.AnimationRecordNbtCodec;
import io.github.kxyr0.echoanimation.forge.playback.nbt.AnimationSubjectNbtCodec;
import io.github.kxyr0.echoanimation.forge.playback.state.AnimationRuntimeTransientSanitizer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Optional;

/** NBT codec for durable animation runtime records. */
final class AnimationRuntimeNbtCodec {
    private final AnimationDefinitionNbtCodec definitionCodec = new AnimationDefinitionNbtCodec();
    private final AnimationRecordNbtCodec recordCodec = new AnimationRecordNbtCodec();
    private final AnimationSubjectNbtCodec subjectCodec = new AnimationSubjectNbtCodec();

    CompoundTag write(AnimationRuntimeSavedData data, CompoundTag tag) {
        data = AnimationRuntimeTransientSanitizer.snapshotForPersistence(data);
        tag.putLong("RepositoryRevision", data.repositoryRevision);
        tag.put( "Subjects", list(data.subjects.values().stream().map(subjectCodec::subjectTag).toList()));
        tag.put( "ActivePlaybacks", list(data.activePlaybacks.values().stream().map(this::playbackTag).toList()));
        tag.put( "TerminalPlaybacks", list( data.terminalPlaybacks.values().stream() .map(recordCodec::terminalTag)
                                .toList()));
        tag.put( "CommandLedger", list(data.commandLedger.values().stream().map(recordCodec::commandTag).toList()));
        tag.put( "DefinitionArchive", list( data.definitionArchive.values().stream() .map(definitionCodec::archiveTag)
                                .toList()));
        return tag;
    }

    void read(CompoundTag tag, AnimationRuntimeSavedData data) {
        data.repositoryRevision = Math.max(1L, tag.getLong("RepositoryRevision"));
        for (Tag item : tag.getList("Subjects", Tag.TAG_COMPOUND)) {
            var record = subjectCodec.subject((CompoundTag) item);
            data.subjects.put(record.subjectId, record);
        }
        for (Tag item : tag.getList("ActivePlaybacks", Tag.TAG_COMPOUND)) {
            PlaybackRecord record = playback((CompoundTag) item);
            data.activePlaybacks.put(record.playbackId, record);
        }
        for (Tag item : tag.getList("TerminalPlaybacks", Tag.TAG_COMPOUND)) {
            TerminalRecord record = recordCodec.terminal((CompoundTag) item);
            data.terminalPlaybacks.put(record.playbackId, record);
        }
        for (Tag item : tag.getList("CommandLedger", Tag.TAG_COMPOUND)) {
            CommandRecord record = recordCodec.command((CompoundTag) item);
            data.commandLedger.put(record.commandId, record);
        }
        for (Tag item : tag.getList("DefinitionArchive", Tag.TAG_COMPOUND)) {
            ArchivedDefinition archive = definitionCodec.archive((CompoundTag) item);
            data.definitionArchive.put(archive.definition().fingerprint(), archive);
        }
    }

    private CompoundTag playbackTag(PlaybackRecord record) {
        CompoundTag tag = new CompoundTag();
        ids(record, tag);
        tag.putString("EntryClip", record.entryClipId.value());
        tag.putString("MainClip", record.mainClipId.value());
        record.exitClipId.ifPresent(id -> tag.putString("ExitClip", id.value()));
        record.entrySegmentId.ifPresent(id -> tag.putString("EntrySegment", id));
        record.mainSegmentId.ifPresent(id -> tag.putString("MainSegment", id));
        record.exitSegmentId.ifPresent(id -> tag.putString("ExitSegment", id));
        record.mainPointId.ifPresent(id -> tag.putString("MainPoint", id));
        tag.putString("Phase", record.phase.name());
        tag.putLong("PhaseEnteredTick", record.phaseEnteredTick);
        tag.putLong("PhaseElapsedTicks", record.phaseElapsedTicks);
        tag.putDouble("SampleElapsedTicks", record.sampleElapsedTicks);
        tag.putLong("MainLoopIteration", record.mainLoopIteration);
        tag.putDouble("PlaybackSpeed", record.playbackSpeed);
        tag.putBoolean("ExitRequested", record.exitRequested);
        tag.putBoolean("UsedAuthoredEntry", record.usedAuthoredEntry);
        tag.putBoolean("FallbackExit", record.fallbackExit);
        record.selectedExitPoint.ifPresent(point -> tag.putString("SelectedExitPoint", point));
        tag.putString("TargetState", record.targetState.value());
        tag.putLong("PlaybackRevision", record.playbackRevision);
        tag.putString("OwnerId", record.ownerId.value());
        tag.put("Leases", leaseTags(record));
        tag.put( "DeliveredMarkers", strings(record.deliveredMarkers.stream().map(AnimationMarkerId::value).toList()));
        return tag;
    }

    private void ids(PlaybackRecord record, CompoundTag tag) {
        tag.putString("PlaybackId", record.playbackId.value());
        tag.putString("AnimationId", record.animationId.value());
        tag.putString("SubjectId", record.subjectId.value());
        tag.putString("CommandId", record.commandId.value());
        tag.putString("DefinitionFingerprint", record.definitionFingerprint);
    }

    private ListTag leaseTags(PlaybackRecord record) {
        ListTag leases = new ListTag();
        record.leases.forEach( (channel, lease) -> {
                    CompoundTag item = new CompoundTag();
                    item.putString("Channel", channel.value());
                    item.putString("Lease", lease.value());
                    leases.add(item);
                });
        return leases;
    }

    private PlaybackRecord playback(CompoundTag tag) {
        PlaybackRecord record = new PlaybackRecord();
        record.playbackId = AnimationPlaybackId.parse(tag.getString("PlaybackId"));
        record.animationId = AnimationId.parse(tag.getString("AnimationId"));
        record.subjectId = new AnimationSubjectId(tag.getString("SubjectId"));
        record.commandId = AnimationCommandId.parse(tag.getString("CommandId"));
        record.definitionFingerprint = tag.getString("DefinitionFingerprint");
        record.entryClipId = AnimationClipId.parse(tag.getString("EntryClip"));
        record.mainClipId = AnimationClipId.parse(tag.getString("MainClip"));
        record.exitClipId = optionalClip(tag, "ExitClip");
        record.entrySegmentId = optionalString(tag, "EntrySegment");
        record.mainSegmentId = optionalString(tag, "MainSegment");
        record.exitSegmentId = optionalString(tag, "ExitSegment");
        record.mainPointId = optionalString(tag, "MainPoint");
        record.phase = AnimationPhase.valueOf(tag.getString("Phase"));
        record.phaseEnteredTick = tag.getLong("PhaseEnteredTick");
        record.phaseElapsedTicks = tag.getLong("PhaseElapsedTicks");
        record.sampleElapsedTicks = sampleElapsedTicks(tag, record.phaseElapsedTicks, playbackSpeed(tag));
        record.mainLoopIteration = tag.getLong("MainLoopIteration");
        record.playbackSpeed = playbackSpeed(tag);
        record.exitRequested = tag.getBoolean("ExitRequested");
        record.usedAuthoredEntry = tag.getBoolean("UsedAuthoredEntry");
        record.fallbackExit = tag.getBoolean("FallbackExit");
        record.selectedExitPoint = optionalString(tag, "SelectedExitPoint");
        record.targetState = new AnimationStateId(tag.getString("TargetState"));
        record.playbackRevision = tag.getLong("PlaybackRevision");
        record.ownerId = new AnimationOwnerId(tag.getString("OwnerId"));
        readLeases(tag, record);
        tag.getList("DeliveredMarkers", Tag.TAG_STRING) .forEach( item ->
                                record.deliveredMarkers.add( AnimationMarkerId.parse(item.getAsString())));
        return record;
    }

    private void readLeases(CompoundTag tag, PlaybackRecord record) {
        for (Tag item : tag.getList("Leases", Tag.TAG_COMPOUND)) {
            CompoundTag lease = (CompoundTag) item;
            record.leases.put( new AnimationChannelId(lease.getString("Channel")),
                    new AnimationLeaseId(lease.getString("Lease")));
        }
    }

    private ListTag list(List<CompoundTag> items) {
        ListTag list = new ListTag();
        items.forEach(list::add);
        return list;
    }

    private ListTag strings(List<String> items) {
        ListTag list = new ListTag();
        items.forEach(item -> list.add(StringTag.valueOf(item)));
        return list;
    }

    private Optional<String> optionalString(CompoundTag tag, String key) {
        return tag.contains(key) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    private Optional<AnimationClipId> optionalClip(CompoundTag tag, String key) {
        return optionalString(tag, key).map(AnimationClipId::parse);
    }

    private double playbackSpeed(CompoundTag tag) {
        if (!tag.contains("PlaybackSpeed")) {
            return 1.0D;
        }
        double value = tag.getDouble("PlaybackSpeed");
        return Double.isFinite(value) && value > 0.0D ? value : 1.0D;
    }

    private double sampleElapsedTicks( CompoundTag tag, long phaseElapsedTicks, double playbackSpeed) {
        if (!tag.contains("SampleElapsedTicks")) {
            return Math.max(0.0D, phaseElapsedTicks * playbackSpeed);
        }
        double value = tag.getDouble("SampleElapsedTicks");
        return Double.isFinite(value) && value >= 0.0D ? value : Math.max(0.0D, phaseElapsedTicks * playbackSpeed);
    }
}
