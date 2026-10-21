package io.github.kxyr0.echoanimation.forge.playback;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.ArchivedDefinition;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.CommandRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.TerminalRecord;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Durable A3 runtime state stored in the server world.
 */
public final class AnimationRuntimeSavedData extends SavedData {
    private static final String NAME = "echo_animation_runtime_v1";
    public final Map<AnimationSubjectId, SubjectRecord> subjects = new LinkedHashMap<>();
    public final Map<AnimationPlaybackId, PlaybackRecord> activePlaybacks = new LinkedHashMap<>();
    public final Map<AnimationPlaybackId, TerminalRecord> terminalPlaybacks = new LinkedHashMap<>();
    public final Map<AnimationCommandId, CommandRecord> commandLedger = new LinkedHashMap<>();
    public final Map<String, ArchivedDefinition> definitionArchive = new LinkedHashMap<>();
    public long repositoryRevision = 1L;

    public static AnimationRuntimeSavedData load(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage()
                .computeIfAbsent(AnimationRuntimeSavedData::load, AnimationRuntimeSavedData::new, NAME);
    }

    static AnimationRuntimeSavedData load(CompoundTag tag) {
        AnimationRuntimeSavedData data = new AnimationRuntimeSavedData();
        new AnimationRuntimeNbtCodec().read(tag, data);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        return new AnimationRuntimeNbtCodec().write(this, tag);
    }

    public SubjectRecord ensureSubject(AnimationSubjectId subjectId) {
        SubjectRecord existing = subjects.get(subjectId);
        if (existing != null) {
            return existing;
        }
        SubjectRecord created = new SubjectRecord(subjectId);
        subjects.put(subjectId, created);
        setDirtyRevision();
        return created;
    }

    public void setDirtyRevision() {
        repositoryRevision++;
        setDirty();
    }
}
