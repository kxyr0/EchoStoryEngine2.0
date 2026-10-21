package io.github.kxyr0.echoanimation.forge.resource.service;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FacePoseId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FacialAnimationId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectLifecycleOwnerId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningRequest;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningResult;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketSample;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echoanimation.forge.network.AnimationNetworkChannel;
import io.github.kxyr0.echoanimation.forge.playback.AnimationPlaybackHost;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/** Public service view over the current catalog and optional server playback host. */
public final class CatalogAnimationService implements AnimationService {
    private final AnimationCatalogState catalog;
    private final AnimationPlaybackHost playbackHost;

    public CatalogAnimationService( AnimationCatalogState catalog, AnimationPlaybackHost playbackHost) {
        this.catalog = java.util.Objects.requireNonNull(catalog, "catalog");
        this.playbackHost = playbackHost;
    }

    @Override
    public List<FaceRigId> faceRigs() {
        return catalog.current().faceRigs().stream().map(rig -> rig.id()).toList();
    }

    @Override
    public List<FacePoseId> facePoses() {
        return catalog.current().facePoses().stream().map(pose -> pose.id()).toList();
    }

    @Override
    public List<FacialAnimationId> facialAnimations() {
        return catalog.current().facialAnimations().stream() .map(animation -> animation.id()) .toList();
    }

    @Override
    public List<AnimationRigId> animationRigs() {
        return catalog.current().animationRigs().stream().map(rig -> rig.id()).toList();
    }

    @Override
    public List<BoneId> animationRigBones(AnimationRigId rigId) {
        return catalog.current().animationRigs().stream() .filter(rig -> rig.id().equals(rigId)) .findFirst()
                .map(rig -> rig.bones().stream().map(bone -> bone.id()).toList()) .orElse(List.of());
    }

    @Override
    public List<AnimationClipId> animationClips() {
        return catalog.current().animationClips().stream().map(clip -> clip.id()).toList();
    }

    @Override
    public List<AnimationId> animations() {
        return catalog.current().animations().stream().map(animation -> animation.id()).toList();
    }

    @Override
    public Optional<MainMode> animationMainMode(AnimationId animationId) {
        return catalog.current().animations().stream() .filter(animation -> animation.id().equals(animationId))
                .findFirst() .map(animation -> animation.main().mode());
    }

    @Override
    public List<AnimationMarkerDefinition> animationMarkers(AnimationId animationId) {
        return catalog.current().animations().stream() .filter(animation -> animation.id().equals(animationId))
                .findFirst() .map(animation -> animation.markers()) .orElse(List.of());
    }

    @Override
    public List<AnimationProfileId> animationProfiles() {
        return catalog.current().animationProfiles().stream().map(profile -> profile.id()).toList();
    }

    @Override
    public Optional<AnimationAttachmentSocketSample> sampleAttachmentSocket( AnimationSubjectId subjectId,
            Optional<AnimationPlaybackId> playbackId, AnimationAttachmentSocketId socketId) {
        return playbackHost == null ? Optional.empty()
                : playbackHost.sampleAttachmentSocket(subjectId, playbackId, socketId);
    }

    @Override
    public boolean ensureSubject(AnimationSubjectId subjectId) {
        return playbackHost != null && playbackHost.ensureSubject(subjectId);
    }

    @Override
    public AnimationSubjectProvisioningResult ensureSubject( AnimationSubjectProvisioningRequest request) {
        return playbackHost == null ? AnimationService.super.ensureSubject(request)
                : playbackHost.ensureSubject(request);
    }

    @Override
    public List<AnimationSubjectId> subjectsByOwner(AnimationSubjectLifecycleOwnerId ownerId) {
        return playbackHost == null ? List.of() : playbackHost.subjectsByOwner(ownerId);
    }

    @Override
    public boolean retireSubject( AnimationSubjectId subjectId, AnimationSubjectLifecycleOwnerId ownerId) {
        return playbackHost != null && playbackHost.retireSubject(subjectId, ownerId);
    }

    @Override
    public void setBodyPostureLocked(AnimationSubjectId subjectId, boolean locked) {
        if (playbackHost != null) { playbackHost.setBodyPostureLocked(subjectId, locked); }
    }

    @Override
    public boolean bodyPostureLocked(AnimationSubjectId subjectId) {
        return playbackHost != null && playbackHost.bodyPostureLocked(subjectId);
    }

    @Override
    public AnimationPlayResult playAnimation(
            AnimationId animationId, AnimationSubjectId subjectId, AnimationCommandId commandId) {
        return playbackHost == null ? AnimationService.super.playAnimation(animationId, subjectId, commandId)
                : playbackHost.play(animationId, subjectId, commandId);
    }

    @Override
    public AnimationPlayResult playAnimation( AnimationId animationId, AnimationSubjectId subjectId,
            AnimationCommandId commandId, double playbackSpeed) {
        return playbackHost == null ? AnimationService.super.playAnimation(
                        animationId, subjectId, commandId, playbackSpeed)
                : playbackHost.play(animationId, subjectId, commandId, playbackSpeed);
    }

    @Override
    public AnimationStopResult stopAnimation( AnimationSubjectId subjectId, Optional<AnimationId> animationId,
            AnimationCommandId commandId) {
        return playbackHost == null ? AnimationService.super.stopAnimation(subjectId, animationId, commandId)
                : playbackHost.stop(subjectId, animationId, commandId);
    }

    @Override
    public boolean updateAnimationPlaybackSpeed( AnimationSubjectId subjectId, AnimationId animationId,
            AnimationCommandId commandId, double playbackSpeed) {
        return playbackHost != null && playbackHost.updatePlaybackSpeed(subjectId, animationId, playbackSpeed);
    }

    @Override
    public AnimationPlaybackQueryResult queryAnimation( AnimationSubjectId subjectId, AnimationId animationId) {
        return playbackHost == null ? AnimationService.super.queryAnimation(subjectId, animationId)
                : playbackHost.query(subjectId, animationId);
    }

    @Override
    public boolean sendFullSnapshot(ServerPlayer player, AnimationSubjectSyncSnapshot snapshot) {
        return AnimationNetworkChannel.sendFullSnapshot(player, snapshot);
    }

    @Override
    public boolean sendSnapshot(ServerPlayer player, AnimationSubjectSyncSnapshot snapshot) {
        return AnimationNetworkChannel.sendSnapshot(player, snapshot);
    }

    @Override
    public boolean sendSubjectRemoval(
            ServerPlayer player, AnimationSubjectId subjectId, AnimationSyncRevision revision) {
        return AnimationNetworkChannel.sendSubjectRemoval(player, subjectId, revision);
    }
}
