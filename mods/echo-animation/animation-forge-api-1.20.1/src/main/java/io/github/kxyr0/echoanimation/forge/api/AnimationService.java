package io.github.kxyr0.echoanimation.forge.api;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FacePoseId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FacialAnimationId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectLifecycleOwnerId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningRequest;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningResult;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketSample;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public Echo Animation service.
 */
public interface AnimationService {
    List<FaceRigId> faceRigs();

    List<FacePoseId> facePoses();

    List<FacialAnimationId> facialAnimations();

    default List<AnimationRigId> animationRigs() {
        return List.of();
    }

    default List<BoneId> animationRigBones(AnimationRigId rigId) {
        return List.of();
    }

    default List<AnimationClipId> animationClips() {
        return List.of();
    }

    default List<AnimationId> animations() {
        return List.of();
    }

    default Optional<MainMode> animationMainMode(AnimationId animationId) {
        return Optional.empty();
    }

    default List<AnimationMarkerDefinition> animationMarkers(AnimationId animationId) {
        return List.of();
    }

    default List<AnimationProfileId> animationProfiles() {
        return List.of();
    }

    default Optional<AnimationAttachmentSocketSample> sampleAttachmentSocket(
            AnimationSubjectId subjectId,
            AnimationAttachmentSocketId socketId
    ) {
        return sampleAttachmentSocket(subjectId, Optional.empty(), socketId);
    }

    default Optional<AnimationAttachmentSocketSample> sampleAttachmentSocket(
            AnimationSubjectId subjectId,
            Optional<AnimationPlaybackId> playbackId,
            AnimationAttachmentSocketId socketId
    ) {
        return Optional.empty();
    }

    default boolean ensureSubject(AnimationSubjectId subjectId) {
        return false;
    }

    default AnimationSubjectProvisioningResult ensureSubject(AnimationSubjectProvisioningRequest request) {
        return new AnimationSubjectProvisioningResult(
                AnimationSubjectProvisioningResult.Status.SERVICE_UNAVAILABLE,
                request.subjectId(),
                request.profileId(),
                Optional.of(request.provenance()),
                "EANIM-A4-UNAVAILABLE",
                "Echo Animation provisioning service is unavailable.");
    }

    default List<AnimationSubjectId> subjectsByOwner(AnimationSubjectLifecycleOwnerId ownerId) {
        return List.of();
    }

    default boolean retireSubject(AnimationSubjectId subjectId, AnimationSubjectLifecycleOwnerId ownerId) {
        return false;
    }

    /**
     * Locks the authoritative BODY slot to an external physical posture such as sitting.
     * While locked, active BODY playback is cancelled and new BODY playback is rejected.
     */
    default void setBodyPostureLocked(AnimationSubjectId subjectId, boolean locked) {
    }

    default boolean bodyPostureLocked(AnimationSubjectId subjectId) {
        return false;
    }

    default AnimationPlayResult playAnimation(
            AnimationId animationId,
            AnimationSubjectId subjectId,
            AnimationCommandId commandId
    ) {
        return new AnimationPlayResult(AnimationPlayResult.Status.DEFINITION_UNAVAILABLE,
                animationId, subjectId, Optional.empty(), Optional.empty(),
                "EANIM-A3-UNAVAILABLE", "Echo Animation playback service is unavailable.");
    }

    default AnimationPlayResult playAnimation(
            AnimationId animationId,
            AnimationSubjectId subjectId,
            AnimationCommandId commandId,
            double playbackSpeed
    ) {
        return playAnimation(animationId, subjectId, commandId);
    }

    default AnimationStopResult stopAnimation(
            AnimationSubjectId subjectId,
            Optional<AnimationId> animationId,
            AnimationCommandId commandId
    ) {
        return new AnimationStopResult(AnimationStopResult.Status.SUBJECT_NOT_FOUND, subjectId, Optional.empty(),
                "EANIM-A3-UNAVAILABLE", "Echo Animation playback service is unavailable.");
    }

    default boolean updateAnimationPlaybackSpeed(
            AnimationSubjectId subjectId,
            AnimationId animationId,
            AnimationCommandId commandId,
            double playbackSpeed
    ) {
        return false;
    }

    default AnimationPlaybackQueryResult queryAnimation(
            AnimationSubjectId subjectId,
            AnimationId animationId
    ) {
        return new AnimationPlaybackQueryResult(AnimationPlaybackQueryResult.Status.DEFINITION_UNAVAILABLE,
                animationId, subjectId, Optional.empty(), Optional.empty(), Optional.empty(), 0L, 0L,
                "EANIM-A5-UNAVAILABLE", "Echo Animation playback query service is unavailable.");
    }

    default boolean sendFullSnapshot(ServerPlayer player, AnimationSubjectSyncSnapshot snapshot) {
        return false;
    }

    default boolean sendSnapshot(ServerPlayer player, AnimationSubjectSyncSnapshot snapshot) {
        return false;
    }

    default boolean sendSubjectRemoval(
            ServerPlayer player,
            AnimationSubjectId subjectId,
            AnimationSyncRevision revision
    ) {
        return false;
    }
}
