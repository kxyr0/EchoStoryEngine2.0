package io.github.kxyr0.echoanimation.forge.playback;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationBlendPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.definition.RecoveryPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.ReplayPolicy;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerId;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationLeaseId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileFingerprint;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectLifecycleOwnerId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvenance;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.ArchivedDefinition;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.CommandRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.TerminalRecord;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationRuntimeSavedDataTest {
    private static final AnimationSubjectId SUBJECT = new AnimationSubjectId("jdh:john");
    private static final AnimationChannelId BODY = new AnimationChannelId("body");
    private static final AnimationStateId IDLE = new AnimationStateId("body/idle");
    private static final AnimationId ANIMATION = AnimationId.parse("jdh:wave");
    private static final AnimationPlaybackId PLAYBACK = AnimationPlaybackId.parse("ptest");
    private static final AnimationCommandId COMMAND = AnimationCommandId.parse("test:play");
    private static final AnimationMarkerId MARKER = AnimationMarkerId.parse("test:take");

    @Test
    void savedDataRoundTripPreservesPlaybackLedgerAndArchive() {
        AnimationRuntimeSavedData data = new AnimationRuntimeSavedData();
        var subject = data.ensureSubject(SUBJECT);
        subject.channelStates.put(BODY, IDLE);
        subject.provenance = Optional.of(new AnimationSubjectProvenance(
                AnimationSubjectLifecycleOwnerId.parse("echo_npc_animation:actor"), "jdh:john"));
        subject.profileId = Optional.of(AnimationProfileId.parse("jdh:standard_humanoid"));
        subject.profileFingerprint = Optional.of(new AnimationProfileFingerprint("profile-fingerprint"));
        data.repositoryRevision = 42L;
        data.activePlaybacks.put(PLAYBACK, playback());
        data.commandLedger.put(COMMAND, command());
        data.definitionArchive.put("fingerprint", new ArchivedDefinition(definition(), 77L));
        data.terminalPlaybacks.put(AnimationPlaybackId.parse("pterminal"), terminal());

        CompoundTag tag = new AnimationRuntimeNbtCodec().write(data, new CompoundTag());
        AnimationRuntimeSavedData restored = new AnimationRuntimeSavedData();
        new AnimationRuntimeNbtCodec().read(tag, restored);

        Assertions.assertEquals(42L, restored.repositoryRevision);
        Assertions.assertEquals(IDLE, restored.subjects.get(SUBJECT).channelStates.get(BODY));
        Assertions.assertEquals(Optional.of(AnimationProfileId.parse("jdh:standard_humanoid")),
                restored.subjects.get(SUBJECT).profileId);
        Assertions.assertEquals("jdh:john", restored.subjects.get(SUBJECT)
                .provenance.orElseThrow().externalCanonicalKey());
        Assertions.assertEquals(AnimationPhase.MAIN, restored.activePlaybacks.get(PLAYBACK).phase);
        Assertions.assertEquals(1.35D, restored.activePlaybacks.get(PLAYBACK).playbackSpeed, 0.0001D);
        Assertions.assertEquals(6.75D, restored.activePlaybacks.get(PLAYBACK).sampleElapsedTicks, 0.0001D);
        Assertions.assertEquals(Optional.of(AnimationClipId.parse("jdh:wave_exit")),
                restored.activePlaybacks.get(PLAYBACK).exitClipId);
        Assertions.assertEquals(Set.of(MARKER), restored.activePlaybacks.get(PLAYBACK).deliveredMarkers);
        Assertions.assertEquals("STARTED", restored.commandLedger.get(COMMAND).status);
        Assertions.assertEquals(ANIMATION, restored.definitionArchive.get("fingerprint").definition().id());
        Assertions.assertEquals(MARKER,
                restored.definitionArchive.get("fingerprint").definition().markers().get(0).id());
        Assertions.assertEquals(AnimationPlaybackOutcome.COMPLETED,
                restored.terminalPlaybacks.get(AnimationPlaybackId.parse("pterminal")).outcome);
    }

    private PlaybackRecord playback() {
        PlaybackRecord record = new PlaybackRecord();
        record.playbackId = PLAYBACK;
        record.animationId = ANIMATION;
        record.subjectId = SUBJECT;
        record.commandId = COMMAND;
        record.definitionFingerprint = "fingerprint";
        record.entryClipId = AnimationClipId.parse("jdh:wave_enter");
        record.mainClipId = AnimationClipId.parse("jdh:wave_loop");
        record.exitClipId = Optional.of(AnimationClipId.parse("jdh:wave_exit"));
        record.phase = AnimationPhase.MAIN;
        record.phaseEnteredTick = 10L;
        record.phaseElapsedTicks = 5L;
        record.sampleElapsedTicks = 6.75D;
        record.mainLoopIteration = 2L;
        record.playbackSpeed = 1.35D;
        record.exitRequested = true;
        record.selectedExitPoint = Optional.of("neutral");
        record.targetState = IDLE;
        record.playbackRevision = 3L;
        record.ownerId = new AnimationOwnerId("echo_animation:ptest");
        record.leases.put(BODY, new AnimationLeaseId("playback/ptest/body"));
        record.deliveredMarkers.add(MARKER);
        return record;
    }

    private CommandRecord command() {
        CommandRecord record = new CommandRecord();
        record.commandId = COMMAND;
        record.operation = "play";
        record.argumentsFingerprint = "play|jdh:wave|jdh:john";
        record.status = "STARTED";
        record.playbackId = Optional.of(PLAYBACK);
        record.diagnosticCode = "EANIM-A3-STARTED";
        record.message = "Started animation.";
        return record;
    }

    private TerminalRecord terminal() {
        TerminalRecord record = new TerminalRecord();
        record.playbackId = AnimationPlaybackId.parse("pterminal");
        record.animationId = ANIMATION;
        record.subjectId = SUBJECT;
        record.outcome = AnimationPlaybackOutcome.COMPLETED;
        record.finalRevision = 4L;
        record.completionTick = 99L;
        record.commandId = COMMAND;
        return record;
    }

    private AnimationDefinition definition() {
        return new AnimationDefinition(ANIMATION, Set.of(BODY), Set.of(IDLE),
                List.of(new AnimationDefinition.EntryVariant(IDLE, AnimationClipId.parse("jdh:wave_enter"), 1L)),
                new AnimationDefinition.MainPhase(AnimationClipId.parse("jdh:wave_loop"), MainMode.LOOP_UNTIL_STOP,
                        12L, List.of(new AnimationDefinition.ExitPoint("neutral", 0.0D))),
                List.of(new AnimationDefinition.ExitVariant("neutral", AnimationClipId.parse("jdh:wave_exit"), 1L)),
                List.of(new AnimationMarkerDefinition(MARKER, AnimationPhase.MAIN, 4L)),
                IDLE, RecoveryPolicy.BOUNDED, 4, ReplayPolicy.REJECT_IF_RUNNING,
                AnimationBlendPolicy.NONE, "fingerprint");
    }
}
