package io.github.kxyr0.echoanimation.forge.playback;

import io.github.kxyr0.echoanimation.forge.playback.result.AnimationPlayFailures;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectLifecycleOwnerId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningRequest;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningResult;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketSample;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.flow.AnimationPlaybackCommandFlow;
import io.github.kxyr0.echoanimation.forge.playback.flow.AnimationPlaybackQueryFlow;
import io.github.kxyr0.echoanimation.forge.playback.flow.AnimationPlaybackSpeedFlow;
import io.github.kxyr0.echoanimation.forge.playback.flow.AnimationPlaybackTickFlow;
import io.github.kxyr0.echoanimation.forge.playback.provisioning.AnimationSubjectChannelInitializer;
import io.github.kxyr0.echoanimation.forge.playback.provisioning.AnimationSubjectProvisioningFlow;
import io.github.kxyr0.echoanimation.forge.playback.state.AnimationRuntimeTransientSanitizer;
import io.github.kxyr0.echoanimation.forge.playback.state.BodyPostureLockController;
import io.github.kxyr0.echoanimation.forge.playback.sync.AnimationSubjectSnapshotPublisher;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Optional;

public final class AnimationPlaybackHost {
    private final AnimationCatalogState catalogState;
    private final AnimationPlaybackCommandFlow commandFlow = new AnimationPlaybackCommandFlow();
    private final AnimationPlaybackQueryFlow queryFlow = new AnimationPlaybackQueryFlow();
    private final AnimationPlaybackSpeedFlow speedFlow = new AnimationPlaybackSpeedFlow();
    private final AnimationPlaybackTickFlow tickFlow = new AnimationPlaybackTickFlow(MinecraftForge.EVENT_BUS::post);
    private final AnimationSubjectProvisioningFlow provisioningFlow = new AnimationSubjectProvisioningFlow();
    private final AnimationSubjectSnapshotPublisher publisher = new AnimationSubjectSnapshotPublisher();
    private final AnimationSubjectChannelInitializer channelInitializer = new AnimationSubjectChannelInitializer();
    private final AnimationSocketSampler socketSampler = new AnimationSocketSampler();
    private final BodyPostureLockController bodyPostures = new BodyPostureLockController();
    private AnimationRuntimeSavedData data;
    private MinecraftServer server;

    public AnimationPlaybackHost(AnimationCatalogState catalogState) {
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
    }

    public void start(MinecraftServer minecraftServer) {
        server = java.util.Objects.requireNonNull(minecraftServer, "server");
        data = AnimationRuntimeSavedData.load(server);
        AnimationRuntimeTransientSanitizer.sanitize(data);
        publishAll(true);
    }

    public void stop() {
        bodyPostures.clear();
        server = null;
        data = null;
    }

    public boolean ensureSubject(AnimationSubjectId subjectId) {
        if (!ready()) { return false; }
        boolean created = !data.subjects.containsKey(subjectId);
        SubjectRecord subject = data.ensureSubject(subjectId);
        if (channelInitializer.inferChannels(data, catalogState.current(), subject) || created) {
            publish(subject.subjectId, true);
        }
        return true;
    }

    public AnimationSubjectProvisioningResult ensureSubject( AnimationSubjectProvisioningRequest request) {
        if (!ready()) {
            return new AnimationSubjectProvisioningResult(
                    AnimationSubjectProvisioningResult.Status.SERVICE_UNAVAILABLE, request.subjectId(),
                    request.profileId(), Optional.of(request.provenance()), "EANIM-A4-UNAVAILABLE",
                    "Echo Animation provisioning service is unavailable.");
        }
        return provisioningFlow.ensure(provisioningContext(), request);
    }

    public List<AnimationSubjectId> subjectsByOwner(AnimationSubjectLifecycleOwnerId ownerId) {
        return ready() ? provisioningFlow.subjectsByOwner(data, ownerId) : List.of();
    }

    public boolean retireSubject( AnimationSubjectId subjectId, AnimationSubjectLifecycleOwnerId ownerId) {
        if (!ready() || !provisioningFlow.retire(provisioningContext(), subjectId, ownerId)) { return false; }
        bodyPostures.remove(subjectId);
        return true;
    }

    public void setBodyPostureLocked(AnimationSubjectId subjectId, boolean locked) {
        if (ready() && bodyPostures.setLocked(data, subjectId, locked)) {
            publish(subjectId, false);
        }
    }

    public boolean bodyPostureLocked(AnimationSubjectId subjectId) {
        return bodyPostures.locked(subjectId);
    }

    public Optional<AnimationAttachmentSocketSample> sampleAttachmentSocket(
            AnimationSubjectId subjectId, AnimationAttachmentSocketId socketId) {
        return sampleAttachmentSocket(subjectId, Optional.empty(), socketId);
    }

    public Optional<AnimationAttachmentSocketSample> sampleAttachmentSocket( AnimationSubjectId subjectId,
            Optional<AnimationPlaybackId> playbackId, AnimationAttachmentSocketId socketId) {
        return ready() ? socketSampler.sample( data, catalogState.current(), new AnimationSocketSampler.Request(
                                subjectId, playbackId, socketId, serverTick())) : Optional.empty();
    }

    public AnimationPlayResult play(
            AnimationId animationId, AnimationSubjectId subjectId, AnimationCommandId commandId) {
        return play(animationId, subjectId, commandId, 1.0D);
    }

    public AnimationPlayResult play( AnimationId animationId, AnimationSubjectId subjectId,
            AnimationCommandId commandId, double playbackSpeed) {
        if (!ready()) { return AnimationPlayFailures.unavailable(animationId, subjectId); }
        Optional<AnimationDefinition> definition = definition(animationId);
        if (bodyPostures.rejects(subjectId, definition)) { return AnimationPlayFailures.postureLocked(animationId,
                subjectId); }
        var result = commandFlow.play( new AnimationPlaybackCommandFlow.PlayRequest( data, animationId, subjectId,
                                commandId, definition, serverTick(), speedFlow.normalized(playbackSpeed)));
        result.changedSubject().ifPresent(subject -> publish(subject, false));
        return result.result();
    }

    public AnimationStopResult stop( AnimationSubjectId subjectId, Optional<AnimationId> animationId,
            AnimationCommandId commandId) {
        if (!ready()) {
            return new AnimationStopResult( AnimationStopResult.Status.SUBJECT_NOT_FOUND, subjectId, Optional.empty(),
                    "EANIM-A3-UNAVAILABLE", "Echo Animation playback service is unavailable.");
        }
        var result = commandFlow.stop( new AnimationPlaybackCommandFlow.StopRequest(
                                data, subjectId, animationId, commandId));
        result.changedSubject().ifPresent(subject -> publish(subject, false));
        return result.result();
    }

    public boolean updatePlaybackSpeed( AnimationSubjectId subjectId, AnimationId animationId, double playbackSpeed) {
        if (!ready()) { return false; }
        Optional<AnimationSubjectId> changed = speedFlow.update(data, subjectId, animationId, playbackSpeed);
        changed.ifPresent(subject -> publish(subject, false));
        return changed.isPresent();
    }

    public AnimationPlaybackQueryResult query( AnimationSubjectId subjectId, AnimationId animationId) {
        if (!ready()) {
            return AnimationPlaybackQueryFlow.unavailable(animationId, subjectId);
        }
        return queryFlow.query(data, catalogState.current().animations(), subjectId, animationId);
    }

    public void tick() {
        if (ready()) {
            tickFlow.tick(data, serverTick()).forEach(subject -> publish(subject, false));
            provisioningFlow.applyPendingRebinds(provisioningContext());
        }
    }

    public void sendFullSnapshots(ServerPlayer player) {
        if (ready()) {
            publisher.sendFullSnapshots(data, catalogState.current(), server, player);
        }
    }

    private Optional<AnimationDefinition> definition(AnimationId animationId) {
        return catalogState.current().animations().stream() .filter(item -> item.id().equals(animationId)) .findFirst();
    }

    private void publish(AnimationSubjectId subjectId, boolean full) {
        publisher.publish(data, catalogState.current(), server, subjectId, full);
    }

    private AnimationSubjectProvisioningFlow.Context provisioningContext() {
        return new AnimationSubjectProvisioningFlow.Context( data, catalogState.current(), this::serverTick,
                subject -> publish(subject, true), this::sendRemoval);
    }

    private void sendRemoval( AnimationSubjectId subjectId,
            io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision revision) {
        if (server != null) {
            publisher.remove(server, subjectId, revision);
        }
    }

    private void publishAll(boolean full) {
        if (ready()) {
            data.subjects.keySet().forEach(subject -> publish(subject, full));
        }
    }

    private long serverTick() {
        return server == null ? 0L : server.overworld().getGameTime();
    }

    private boolean ready() {
        return server != null && data != null;
    }
}
