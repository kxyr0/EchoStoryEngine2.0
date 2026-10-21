package io.github.kxyr0.echoanimation.forge.playback;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationBlendPolicy;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerId;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.definition.RecoveryPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.ReplayPolicy;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.ArchivedDefinition;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * NBT codec for archived compiled high-level animation definitions.
 */
final class AnimationDefinitionNbtCodec {
    CompoundTag archiveTag(ArchivedDefinition archive) {
        CompoundTag tag = definitionTag(archive.definition());
        tag.putLong("StoredAtTick", archive.storedAtTick());
        return tag;
    }

    ArchivedDefinition archive(CompoundTag tag) {
        return new ArchivedDefinition(definition(tag), tag.getLong("StoredAtTick"));
    }

    private CompoundTag definitionTag(AnimationDefinition definition) {
        CompoundTag tag = new CompoundTag();
        tag.putString("AnimationId", definition.id().value());
        tag.put("Channels", strings(definition.controlledChannels().stream().map(AnimationChannelId::value).toList()));
        tag.put("Sources", strings(definition.allowedSourceStates().stream().map(AnimationStateId::value).toList()));
        tag.put("Entries", list(definition.entryVariants().stream().map(this::entryTag).toList()));
        tag.put("Main", mainTag(definition.main()));
        tag.put("Exits", list(definition.exitVariants().stream().map(this::exitTag).toList()));
        tag.put("Markers", list(definition.markers().stream().map(this::markerTag).toList()));
        tag.putString("TargetState", definition.targetState().value());
        tag.putString("RecoveryPolicy", definition.recoveryPolicy().name());
        tag.putInt("CheckpointIntervalTicks", definition.checkpointIntervalTicks());
        tag.putString("ReplayPolicy", definition.replayPolicy().name());
        tag.putInt("FallbackCrossfadeTicks", definition.blendPolicy().fallbackCrossfadeTicks());
        tag.putInt("BlendInTicks", definition.blendPolicy().blendInTicks());
        tag.putInt("BlendOutTicks", definition.blendPolicy().blendOutTicks());
        putOptional(tag, "MaskId", definition.blendPolicy().maskId());
        tag.putString("Fingerprint", definition.fingerprint());
        return tag;
    }

    private AnimationDefinition definition(CompoundTag tag) {
        return new AnimationDefinition(AnimationId.parse(tag.getString("AnimationId")),
                channels(tag.getList("Channels", Tag.TAG_STRING)),
                states(tag.getList("Sources", Tag.TAG_STRING)),
                tag.getList("Entries", Tag.TAG_COMPOUND).stream().map(item -> entry((CompoundTag) item)).toList(),
                main(tag.getCompound("Main")),
                tag.getList("Exits", Tag.TAG_COMPOUND).stream().map(item -> exit((CompoundTag) item)).toList(),
                tag.getList("Markers", Tag.TAG_COMPOUND).stream().map(item -> marker((CompoundTag) item)).toList(),
                new AnimationStateId(tag.getString("TargetState")),
                RecoveryPolicy.valueOf(tag.getString("RecoveryPolicy")),
                tag.getInt("CheckpointIntervalTicks"),
                ReplayPolicy.valueOf(tag.getString("ReplayPolicy")),
                new AnimationBlendPolicy(tag.getInt("FallbackCrossfadeTicks"), tag.getInt("BlendInTicks"),
                        tag.getInt("BlendOutTicks"), optional(tag, "MaskId")),
                tag.getString("Fingerprint"));
    }

    private CompoundTag entryTag(AnimationDefinition.EntryVariant entry) {
        CompoundTag tag = new CompoundTag();
        tag.putString("SourceState", entry.sourceState().value());
        tag.putString("ClipId", entry.clipId().value());
        putOptional(tag, "SegmentId", entry.segmentId());
        tag.putLong("DurationTicks", entry.durationTicks());
        return tag;
    }

    private AnimationDefinition.EntryVariant entry(CompoundTag tag) {
        return new AnimationDefinition.EntryVariant(new AnimationStateId(tag.getString("SourceState")),
                AnimationClipId.parse(tag.getString("ClipId")), optional(tag, "SegmentId"),
                tag.getLong("DurationTicks"));
    }

    private CompoundTag mainTag(AnimationDefinition.MainPhase main) {
        CompoundTag tag = new CompoundTag();
        tag.putString("ClipId", main.clipId().value());
        putOptional(tag, "SegmentId", main.segmentId());
        putOptional(tag, "PointId", main.pointId());
        tag.putString("Mode", main.mode().name());
        tag.putLong("DurationTicks", main.durationTicks());
        tag.put("ExitPoints", list(main.exitPoints().stream().map(this::exitPointTag).toList()));
        return tag;
    }

    private AnimationDefinition.MainPhase main(CompoundTag tag) {
        return new AnimationDefinition.MainPhase(AnimationClipId.parse(tag.getString("ClipId")),
                optional(tag, "SegmentId"), optional(tag, "PointId"),
                MainMode.valueOf(tag.getString("Mode")), tag.getLong("DurationTicks"),
                tag.getList("ExitPoints", Tag.TAG_COMPOUND).stream()
                        .map(item -> exitPoint((CompoundTag) item)).toList());
    }

    private CompoundTag exitPointTag(AnimationDefinition.ExitPoint point) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", point.id());
        tag.putDouble("Progress", point.progress());
        putOptional(tag, "SourcePointId", point.sourcePointId());
        return tag;
    }

    private AnimationDefinition.ExitPoint exitPoint(CompoundTag tag) {
        return new AnimationDefinition.ExitPoint(tag.getString("Id"), tag.getDouble("Progress"),
                optional(tag, "SourcePointId"));
    }

    private CompoundTag exitTag(AnimationDefinition.ExitVariant exit) {
        CompoundTag tag = new CompoundTag();
        tag.putString("ExitPointId", exit.exitPointId());
        tag.putString("ClipId", exit.clipId().value());
        putOptional(tag, "SegmentId", exit.segmentId());
        tag.putLong("DurationTicks", exit.durationTicks());
        return tag;
    }

    private AnimationDefinition.ExitVariant exit(CompoundTag tag) {
        return new AnimationDefinition.ExitVariant(tag.getString("ExitPointId"),
                AnimationClipId.parse(tag.getString("ClipId")), optional(tag, "SegmentId"),
                tag.getLong("DurationTicks"));
    }

    private CompoundTag markerTag(AnimationMarkerDefinition marker) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", marker.id().value());
        tag.putString("Phase", marker.phase().name());
        tag.putLong("PhaseTick", marker.phaseTick());
        return tag;
    }

    private AnimationMarkerDefinition marker(CompoundTag tag) {
        return new AnimationMarkerDefinition(AnimationMarkerId.parse(tag.getString("Id")),
                AnimationPhase.valueOf(tag.getString("Phase")), tag.getLong("PhaseTick"));
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

    private Set<AnimationChannelId> channels(ListTag list) {
        Set<AnimationChannelId> result = new LinkedHashSet<>();
        list.forEach(item -> result.add(new AnimationChannelId(item.getAsString())));
        return result;
    }

    private Set<AnimationStateId> states(ListTag list) {
        Set<AnimationStateId> result = new LinkedHashSet<>();
        list.forEach(item -> result.add(new AnimationStateId(item.getAsString())));
        return result;
    }

    private void putOptional(CompoundTag tag, String key, Optional<String> value) {
        value.ifPresent(text -> tag.putString(key, text));
    }

    private Optional<String> optional(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
    }
}
