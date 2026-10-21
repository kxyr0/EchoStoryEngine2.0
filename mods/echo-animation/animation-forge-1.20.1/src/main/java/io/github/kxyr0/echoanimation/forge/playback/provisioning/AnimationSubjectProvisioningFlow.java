package io.github.kxyr0.echoanimation.forge.playback.provisioning;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileFingerprint;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.profile.definition.AnimationProfileDefinition;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectLifecycleOwnerId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningRequest;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningResult;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.TerminalRecord;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * A4 subject provisioning, profile rebind and adapter-owned retirement flow.
 */
public final class AnimationSubjectProvisioningFlow {
    public AnimationSubjectProvisioningResult ensure(Context context, AnimationSubjectProvisioningRequest request) {
        Optional<AnimationProfileDefinition> profile = profile(context.resources(), request.profileId());
        if (profile.isEmpty()) {
            return provisioning(request, AnimationSubjectProvisioningResult.Status.PROFILE_NOT_FOUND,
                    "EANIM-A4-PROFILE-NOT-FOUND", "Animation profile not found: " + request.profileId());
        }
        SubjectRecord existing = context.data().subjects.get(request.subjectId());
        if (existing == null) {
            SubjectRecord created = context.data().ensureSubject(request.subjectId());
            created.provenance = Optional.of(request.provenance());
            applyProfile(context, created, profile.orElseThrow());
            context.publishFull().accept(created.subjectId);
            return provisioning(request, AnimationSubjectProvisioningResult.Status.CREATED,
                    "EANIM-A4-CREATED", "Animation subject created.");
        }
        Optional<AnimationSubjectProvisioningResult> conflict = conflict(existing, request);
        if (conflict.isPresent()) {
            return conflict.orElseThrow();
        }
        AnimationProfileDefinition nextProfile = profile.orElseThrow();
        if (sameProfile(existing, nextProfile)) {
            return provisioning(request, AnimationSubjectProvisioningResult.Status.EXISTING,
                    "EANIM-A4-EXISTING", "Animation subject already exists.");
        }
        if (hasActivePlayback(context.data(), existing.subjectId)) {
            existing.pendingProfileId = Optional.of(nextProfile.id());
            existing.pendingProfileFingerprint = Optional.of(nextProfile.fingerprint());
            context.data().setDirtyRevision();
            return provisioning(request, AnimationSubjectProvisioningResult.Status.PROFILE_REBIND_REQUIRED,
                    "EANIM-A4-REBIND-PENDING", "Profile rebind is pending until active playbacks are terminal.");
        }
        applyProfile(context, existing, nextProfile);
        context.publishFull().accept(existing.subjectId);
        return provisioning(request, AnimationSubjectProvisioningResult.Status.EXISTING,
                "EANIM-A4-REBIND-APPLIED", "Safe profile rebind applied.");
    }

    public List<AnimationSubjectId> subjectsByOwner(
            AnimationRuntimeSavedData data,
            AnimationSubjectLifecycleOwnerId ownerId
    ) {
        return data.subjects.values().stream()
                .filter(subject -> subject.provenance
                        .map(provenance -> provenance.lifecycleOwnerId().equals(ownerId)).orElse(false))
                .map(subject -> subject.subjectId)
                .sorted()
                .toList();
    }

    public boolean retire(Context context, AnimationSubjectId subjectId, AnimationSubjectLifecycleOwnerId ownerId) {
        SubjectRecord subject = context.data().subjects.get(subjectId);
        if (subject == null || subject.provenance.isEmpty()
                || !subject.provenance.orElseThrow().lifecycleOwnerId().equals(ownerId)) {
            return false;
        }
        for (PlaybackRecord playback : activePlaybacks(context.data(), subjectId)) {
            context.data().activePlaybacks.remove(playback.playbackId);
            context.data().terminalPlaybacks.put(playback.playbackId, terminal(context, playback));
        }
        context.data().subjects.remove(subjectId);
        context.data().setDirtyRevision();
        context.removeSubject().accept(subjectId, new AnimationSyncRevision(context.data().repositoryRevision));
        return true;
    }

    public void applyPendingRebinds(Context context) {
        for (SubjectRecord subject : context.data().subjects.values()) {
            if (subject.pendingProfileId.isEmpty() || hasActivePlayback(context.data(), subject.subjectId)) {
                continue;
            }
            profile(context.resources(), subject.pendingProfileId.orElseThrow()).ifPresent(profile -> {
                applyProfile(context, subject, profile);
                context.publishFull().accept(subject.subjectId);
            });
        }
    }

    private Optional<AnimationSubjectProvisioningResult> conflict(
            SubjectRecord existing,
            AnimationSubjectProvisioningRequest request
    ) {
        if (existing.provenance.isPresent() && !existing.provenance.orElseThrow().equals(request.provenance())) {
            return Optional.of(provisioning(request, AnimationSubjectProvisioningResult.Status.PROVENANCE_CONFLICT,
                    "EANIM-A4-PROVENANCE-CONFLICT", "Animation subject belongs to different provenance."));
        }
        if (existing.provenance.isEmpty()) {
            return Optional.of(provisioning(request, AnimationSubjectProvisioningResult.Status.PROVENANCE_CONFLICT,
                    "EANIM-A4-PROVENANCE-CONFLICT", "Animation subject has no adapter provenance."));
        }
        return Optional.empty();
    }

    private Optional<AnimationProfileDefinition> profile(AnimationResourceSet resources, AnimationProfileId profileId) {
        return resources.animationProfiles().stream().filter(profile -> profile.id().equals(profileId)).findFirst();
    }

    private void applyProfile(Context context, SubjectRecord subject, AnimationProfileDefinition profile) {
        subject.profileId = Optional.of(profile.id());
        subject.profileFingerprint = Optional.of(profile.fingerprint());
        subject.pendingProfileId = Optional.empty();
        subject.pendingProfileFingerprint = Optional.empty();
        subject.channelStates.clear();
        subject.channelStates.putAll(profile.initialStates());
        subject.revision++;
        context.data().setDirtyRevision();
    }

    private boolean sameProfile(SubjectRecord subject, AnimationProfileDefinition profile) {
        return subject.profileId.filter(profile.id()::equals).isPresent()
                && subject.profileFingerprint.map(AnimationProfileFingerprint::value)
                        .filter(profile.fingerprint().value()::equals).isPresent()
                && subject.pendingProfileId.isEmpty();
    }

    private boolean hasActivePlayback(AnimationRuntimeSavedData data, AnimationSubjectId subjectId) {
        return data.activePlaybacks.values().stream().anyMatch(playback -> playback.subjectId.equals(subjectId));
    }

    private List<PlaybackRecord> activePlaybacks(AnimationRuntimeSavedData data, AnimationSubjectId subjectId) {
        return data.activePlaybacks.values().stream()
                .filter(playback -> playback.subjectId.equals(subjectId))
                .toList();
    }

    private TerminalRecord terminal(Context context, PlaybackRecord playback) {
        TerminalRecord terminal = new TerminalRecord();
        terminal.playbackId = playback.playbackId;
        terminal.animationId = playback.animationId;
        terminal.subjectId = playback.subjectId;
        terminal.outcome = AnimationPlaybackOutcome.SUBJECT_OWNER_REMOVED;
        terminal.finalRevision = playback.playbackRevision + 1L;
        terminal.completionTick = context.serverTick().getAsLong();
        terminal.commandId = AnimationCommandId.parse("system:subject_owner_removed");
        return terminal;
    }

    private AnimationSubjectProvisioningResult provisioning(
            AnimationSubjectProvisioningRequest request,
            AnimationSubjectProvisioningResult.Status status,
            String code,
            String message
    ) {
        return new AnimationSubjectProvisioningResult(status, request.subjectId(), request.profileId(),
                Optional.of(request.provenance()), code, message);
    }

    public record Context(
            AnimationRuntimeSavedData data,
            AnimationResourceSet resources,
            LongSupplier serverTick,
            Consumer<AnimationSubjectId> publishFull,
            SubjectRemovalSink removeSubject
    ) {
    }

    public interface SubjectRemovalSink {
        void accept(AnimationSubjectId subjectId, AnimationSyncRevision revision);
    }
}
