package io.github.kxyr0.echoanimation.forge.playback.nbt;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.CommandRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.TerminalRecord;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;

/**
 * NBT codec for command ledger and terminal playback records.
 */
public final class AnimationRecordNbtCodec {
    public CompoundTag commandTag(CommandRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("CommandId", record.commandId.value());
        tag.putString("Operation", record.operation);
        tag.putString("ArgumentsFingerprint", record.argumentsFingerprint);
        tag.putString("Status", record.status);
        record.playbackId.ifPresent(id -> tag.putString("PlaybackId", id.value()));
        tag.putString("DiagnosticCode", record.diagnosticCode);
        tag.putString("Message", record.message);
        return tag;
    }

    public CommandRecord command(CompoundTag tag) {
        CommandRecord record = new CommandRecord();
        record.commandId = AnimationCommandId.parse(tag.getString("CommandId"));
        record.operation = tag.getString("Operation");
        record.argumentsFingerprint = tag.getString("ArgumentsFingerprint");
        record.status = tag.getString("Status");
        record.playbackId = optionalPlayback(tag, "PlaybackId");
        record.diagnosticCode = tag.getString("DiagnosticCode");
        record.message = tag.getString("Message");
        return record;
    }

    public CompoundTag terminalTag(TerminalRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("PlaybackId", record.playbackId.value());
        tag.putString("AnimationId", record.animationId.value());
        tag.putString("SubjectId", record.subjectId.value());
        tag.putString("Outcome", record.outcome.name());
        tag.putLong("FinalRevision", record.finalRevision);
        tag.putLong("CompletionTick", record.completionTick);
        tag.putString("CommandId", record.commandId.value());
        return tag;
    }

    public TerminalRecord terminal(CompoundTag tag) {
        TerminalRecord record = new TerminalRecord();
        record.playbackId = AnimationPlaybackId.parse(tag.getString("PlaybackId"));
        record.animationId = AnimationId.parse(tag.getString("AnimationId"));
        record.subjectId = new AnimationSubjectId(tag.getString("SubjectId"));
        record.outcome = AnimationPlaybackOutcome.valueOf(tag.getString("Outcome"));
        record.finalRevision = tag.getLong("FinalRevision");
        record.completionTick = tag.getLong("CompletionTick");
        record.commandId = AnimationCommandId.parse(tag.getString("CommandId"));
        return record;
    }

    private Optional<AnimationPlaybackId> optionalPlayback(CompoundTag tag, String key) {
        return tag.contains(key) ? Optional.of(AnimationPlaybackId.parse(tag.getString(key))) : Optional.empty();
    }
}
