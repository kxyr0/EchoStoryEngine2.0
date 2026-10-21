package io.github.kxyr0.echoanimation.forge.client.dialogue;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FacialAnimationDefinition;
import io.github.kxyr0.echoanimation.forge.face.BlinkTimingOverride;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;

/** Client-local face animation state driven by dialogue cues and persistent subject commands. */
public final class DialoguePortraitAnimationState {
    private static final DialoguePortraitAnimationState SHARED = new DialoguePortraitAnimationState();

    private final Map<String, ActiveFaceAnimation> activeByVisual = new LinkedHashMap<>();
    private final Map<String, ActiveFaceAnimation> activeBySubject = new LinkedHashMap<>();
    private final Map<String, ActiveFaceAnimation> persistentBySubject = new LinkedHashMap<>();
    private final Map<String, BlinkTimingOverride> blinkTimingBySubject = new LinkedHashMap<>();
    private final DialogueFaceSampler sampling = new DialogueFaceSampler();

    public static DialoguePortraitAnimationState shared() {
        return SHARED;
    }

    public static void playPersistentSubjectNow(String subjectId, String faceAnimationId) {
        shared().playPersistentSubject(subjectId, faceAnimationId, sampleTick());
    }

    public static void clearPersistentSubjectNow(String subjectId) {
        shared().clearPersistentSubject(subjectId);
    }

    public static void clearAllPersistentSubjectsNow() {
        shared().clearAllPersistentSubjects();
    }

    public static void setBlinkTimingNow(String subjectId, BlinkTimingOverride timing) {
        shared().setBlinkTiming(subjectId, timing);
    }

    public static void clearBlinkTimingNow(String subjectId) {
        shared().clearBlinkTiming(subjectId);
    }

    void play(String visualId, String faceAnimationId, long gameTick) {
        playPortrait(visualId, faceAnimationId, gameTick);
    }

    void playPortrait(String visualId, String faceAnimationId, long gameTick) {
        play(activeByVisual, visualId, faceAnimationId, gameTick);
    }

    void playSubject(String subjectId, String faceAnimationId, long gameTick) {
        play(activeBySubject, subjectId, faceAnimationId, gameTick);
    }

    public void playPersistentSubject(String subjectId, String faceAnimationId, long gameTick) {
        play(persistentBySubject, subjectId, faceAnimationId, gameTick);
    }

    public void clearPersistentSubject(String subjectId) {
        String key = Objects.requireNonNull(subjectId, "subjectId");
        persistentBySubject.remove(key);
        activeBySubject.remove(key);
        activeByVisual.remove(key);
    }

    public void clearAllPersistentSubjects() {
        persistentBySubject.clear();
        clear();
    }

    public void setBlinkTiming(String subjectId, BlinkTimingOverride timing) {
        blinkTimingBySubject.put( Objects.requireNonNull(subjectId, "subjectId"), Objects.requireNonNull(timing));
    }

    public void clearBlinkTiming(String subjectId) {
        blinkTimingBySubject.remove(subjectId);
    }

    void stopPortrait(String visualId, long gameTick) {
        stop(activeByVisual, visualId, gameTick);
    }

    void stopSubject(String subjectId, long gameTick) {
        stop(activeBySubject, subjectId, gameTick);
    }

    void clear() {
        activeByVisual.clear();
        activeBySubject.clear();
    }

    Optional<BonePose> posePortrait(
            AnimationResourceSet resources, FaceRigDefinition rig, String visualId, long gameTick) {
        return posePortrait(resources, rig, visualId, (double) gameTick);
    }

    Optional<BonePose> posePortrait( AnimationResourceSet resources, FaceRigDefinition rig, String visualId,
            double sampleTick) {
        return pose( activeByVisual, new PoseQuery( resources, rig, visualId,
                        FacialAnimationDefinition.SURFACE_PORTRAIT, sampleTick));
    }

    public Optional<BonePose> poseSubject( AnimationResourceSet resources, FaceRigDefinition rig, String subjectId,
            long gameTick) {
        return poseSubject(resources, rig, subjectId, (double) gameTick);
    }

    public Optional<BonePose> poseSubject( AnimationResourceSet resources, FaceRigDefinition rig, String subjectId,
            double sampleTick) {
        PoseQuery query = new PoseQuery( resources, rig, subjectId, FacialAnimationDefinition.SURFACE_3D, sampleTick);
        Optional<BonePose> persistent = pose(persistentBySubject, query);
        Optional<BonePose> dialogue = pose(activeBySubject, query);
        if (persistent.isPresent() && dialogue.isPresent()) {
            return Optional.of(merge(persistent.orElseThrow(), dialogue.orElseThrow()));
        }
        return dialogue.or(() -> persistent);
    }

    boolean suppressesIdleBlinkPortrait( AnimationResourceSet resources, FaceRigDefinition rig, String visualId) {
        return suppressesIdleBlink(activeByVisual, resources, rig, visualId);
    }

    public boolean suppressesIdleBlinkSubject(
            AnimationResourceSet resources, FaceRigDefinition rig, String subjectId) {
        return suppressesIdleBlink(activeBySubject, resources, rig, subjectId)
                || suppressesIdleBlink(persistentBySubject, resources, rig, subjectId);
    }

    public Optional<BlinkTimingOverride> blinkTiming(String subjectId) {
        return Optional.ofNullable(blinkTimingBySubject.get(subjectId));
    }

    private BonePose merge(BonePose base, BonePose extra) {
        Map<BoneId, Transform3> transforms = new LinkedHashMap<>(base.localTransforms());
        extra.localTransforms() .forEach( (bone, transform) ->
                                transforms.merge(bone, transform, Transform3::plusDelta));
        return new BonePose(transforms);
    }

    private void play( Map<String, ActiveFaceAnimation> target, String key, String faceAnimationId, long gameTick) {
        target.put( Objects.requireNonNull(key, "key"), new ActiveFaceAnimation(
                        Objects.requireNonNull(faceAnimationId, "faceAnimationId"), gameTick, false));
    }

    private void stop(Map<String, ActiveFaceAnimation> target, String key, long gameTick) {
        ActiveFaceAnimation active = target.get(key);
        if (active == null) {
            return;
        }
        target.put(key, new ActiveFaceAnimation(active.animationId(), gameTick, true));
    }

    private Optional<BonePose> pose(Map<String, ActiveFaceAnimation> target, PoseQuery query) {
        ActiveFaceAnimation active = target.get(query.key());
        if (active == null) {
            return Optional.empty();
        }
        Optional<BonePose> pose = sampling.animation(query.resources(), query.rig().id(), active.animationId())
                        .flatMap(animation -> animation.surface(query.surface(), query.rig().id())) .flatMap(
                                animationSurface ->
                                        sampling.sample( target, query.key(), animationSurface, active,
                query.sampleTick()));
        if (pose.isEmpty()) {
            target.remove(query.key());
        }
        return pose;
    }

    private boolean suppressesIdleBlink( Map<String, ActiveFaceAnimation> target, AnimationResourceSet resources,
            FaceRigDefinition rig, String key) {
        ActiveFaceAnimation active = target.get(key);
        if (active == null) {
            return false;
        }
        Optional<FacialAnimationDefinition> animation = sampling.animation(resources, rig.id(), active.animationId());
        if (animation.isEmpty()) {
            target.remove(key);
            return false;
        }
        return animation.orElseThrow().suppressIdleBlink();
    }

    private static long sampleTick() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    private record PoseQuery( AnimationResourceSet resources, FaceRigDefinition rig, String key, String surface,
            double sampleTick) {}

    record ActiveFaceAnimation(String animationId, long startTick, boolean exiting) {
        ActiveFaceAnimation {
            animationId = Objects.requireNonNull(animationId, "animationId");
        }
    }
}
