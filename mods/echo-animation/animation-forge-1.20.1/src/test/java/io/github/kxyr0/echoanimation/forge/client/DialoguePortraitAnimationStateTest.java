package io.github.kxyr0.echoanimation.forge.client.dialogue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FacialAnimationId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.AnimationEndBehavior;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.core.pose.BoneTrack;
import io.github.kxyr0.echoanimation.core.pose.Keyframe;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FacialAnimationDefinition;
import io.github.kxyr0.echoanimation.forge.face.BlinkTimingOverride;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
class DialoguePortraitAnimationStateTest {
    private static final BoneId PUPIL = BoneId.parse("pupil_left");
    private static final FaceRigId RIG_ID = FaceRigId.parse("jdh:john_father");
    private static final FacialAnimationId ANIMATION_ID = FacialAnimationId.parse("jdh:look_left");
    @Test
    void samplesActiveDialogueFaceAnimationByVisualId() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.play(RIG_ID.toString(), ANIMATION_ID.toString(), 100L);
        Transform3 transform = state.posePortrait(resources(RIG_ID), rig(RIG_ID), RIG_ID.toString(), 110L)
                .orElseThrow()
                .transform(PUPIL)
                .orElseThrow();
        assertEquals(0.5D, transform.translation().x());
    }
    @Test
    void rigMismatchReturnsNoPose() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.play(RIG_ID.toString(), ANIMATION_ID.toString(), 100L);
        assertTrue(state.posePortrait(resources(FaceRigId.parse("jdh:other")), rig(RIG_ID),
                RIG_ID.toString(), 110L).isEmpty());
    }
    @Test
    void clearRemovesExplicitAnimationState() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.play(RIG_ID.toString(), ANIMATION_ID.toString(), 100L);
        state.clear();
        assertTrue(state.posePortrait(resources(RIG_ID), rig(RIG_ID), RIG_ID.toString(), 110L).isEmpty());
    }
    @Test
    void clearDoesNotRemovePersistentSubjectAnimationState() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.playPersistentSubject("jdh:john_father", ANIMATION_ID.toString(), 100L);
        state.clear();
        Transform3 transform = state.poseSubject(surfaceResources(), rig(RIG_ID), "jdh:john_father", 110L)
                .orElseThrow()
                .transform(PUPIL)
                .orElseThrow();
        assertEquals(2.0D, transform.translation().x());
    }
    @Test
    void clearPersistentSubjectRemovesOnlyThatSubjectAnimationState() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.playPersistentSubject("jdh:john_father", ANIMATION_ID.toString(), 100L);
        state.playPersistentSubject("jdh:sasha", ANIMATION_ID.toString(), 100L);
        state.clearPersistentSubject("jdh:john_father");
        assertTrue(state.poseSubject(surfaceResources(), rig(RIG_ID), "jdh:john_father", 110L).isEmpty());
        assertTrue(state.poseSubject(surfaceResources(), rig(RIG_ID), "jdh:sasha", 110L).isPresent());
    }
    @Test
    void clearAllPersistentSubjectsRemovesPersistentAnimationState() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.playPersistentSubject("jdh:john_father", ANIMATION_ID.toString(), 100L);
        state.playPersistentSubject("jdh:sasha", ANIMATION_ID.toString(), 100L);
        state.clearAllPersistentSubjects();
        assertTrue(state.poseSubject(surfaceResources(), rig(RIG_ID), "jdh:john_father", 110L).isEmpty());
        assertTrue(state.poseSubject(surfaceResources(), rig(RIG_ID), "jdh:sasha", 110L).isEmpty());
    }
    @Test
    void stopSamplesExitPhaseBeforeRemovingAnimationState() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.playPortrait(RIG_ID.toString(), ANIMATION_ID.toString(), 100L);
        Transform3 held = state.posePortrait(surfaceResources(), rig(RIG_ID), RIG_ID.toString(), 110L)
                .orElseThrow()
                .transform(PUPIL)
                .orElseThrow();
        state.stopPortrait(RIG_ID.toString(), 110L);
        Transform3 exiting = state.posePortrait(surfaceResources(), rig(RIG_ID), RIG_ID.toString(), 112L)
                .orElseThrow()
                .transform(PUPIL)
                .orElseThrow();
        assertEquals(1.0D, held.translation().x());
        assertEquals(0.5D, exiting.translation().x());
        assertTrue(state.posePortrait(surfaceResources(), rig(RIG_ID), RIG_ID.toString(), 115L).isEmpty());
    }
    @Test
    void subjectPoseSamplesThreeDimensionalSurface() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.playSubject("jdh:john_father", ANIMATION_ID.toString(), 100L);
        Transform3 transform = state.poseSubject(surfaceResources(), rig(RIG_ID), "jdh:john_father", 110L)
                .orElseThrow()
                .transform(PUPIL)
                .orElseThrow();
        assertEquals(2.0D, transform.translation().x());
    }
    @Test
    void subjectPoseSamplesBetweenGameTicks() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.playSubject("jdh:john_father", ANIMATION_ID.toString(), 100L);
        Transform3 wholeTick = state.poseSubject(surfaceResources(), rig(RIG_ID), "jdh:john_father", 102.0D)
                .orElseThrow()
                .transform(PUPIL)
                .orElseThrow();
        Transform3 partialTick = state.poseSubject(surfaceResources(), rig(RIG_ID), "jdh:john_father", 102.5D)
                .orElseThrow()
                .transform(PUPIL)
                .orElseThrow();
        assertEquals(1.0D, wholeTick.translation().x());
        assertEquals(1.25D, partialTick.translation().x());
    }
    @Test
    void reportsIdleBlinkSuppressionForActiveSubjectAnimation() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.playSubject("jdh:john_father", ANIMATION_ID.toString(), 100L);
        assertTrue(state.suppressesIdleBlinkSubject(surfaceResources(true), rig(RIG_ID), "jdh:john_father"));
        assertFalse(state.suppressesIdleBlinkSubject(surfaceResources(false), rig(RIG_ID), "jdh:john_father"));
    }
    @Test
    void persistentSubjectAnimationCanSuppressIdleBlink() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        state.playPersistentSubject("jdh:john_father", ANIMATION_ID.toString(), 100L);
        assertTrue(state.suppressesIdleBlinkSubject(surfaceResources(true), rig(RIG_ID), "jdh:john_father"));
    }
    @Test
    void blinkTimingOverrideCanBeSetAndClearedBySubject() {
        DialoguePortraitAnimationState state = new DialoguePortraitAnimationState();
        BlinkTimingOverride timing = new BlinkTimingOverride(34, 5, 8, 9);
        state.setBlinkTiming("jdh:sasha", timing);
        assertEquals(Optional.of(timing), state.blinkTiming("jdh:sasha"));
        state.clearBlinkTiming("jdh:sasha");
        assertTrue(state.blinkTiming("jdh:sasha").isEmpty());
    }
    private static AnimationResourceSet resources(FaceRigId animationRigId) {
        return new AnimationResourceSet(List.of(rig(RIG_ID)), List.of(), List.of(
                new FacialAnimationDefinition(
                        ANIMATION_ID,
                        Optional.of(animationRigId),
                        1.0D,
                        AnimationEndBehavior.HOLD,
                        List.of(new BoneTrack(PUPIL, List.of(
                                 new Keyframe(0.0D, Transform3.IDENTITY, InterpolationKind.LINEAR),
                                 new Keyframe(1.0D, moveX(1.0D), InterpolationKind.LINEAR)))),
                        List.of(), false, "test")));
    }
    private static AnimationResourceSet surfaceResources() {
        return surfaceResources(false);
    }
    private static AnimationResourceSet surfaceResources(boolean suppressIdleBlink) {
        return new AnimationResourceSet(List.of(rig(RIG_ID)), List.of(), List.of(
                new FacialAnimationDefinition(
                        ANIMATION_ID,
                        Optional.empty(),
                        0.0D,
                        AnimationEndBehavior.HOLD,
                        List.of(),
                        List.of(
                                surface(FacialAnimationDefinition.SURFACE_PORTRAIT, 1.0D),
                                surface(FacialAnimationDefinition.SURFACE_3D, 2.0D)),
                        suppressIdleBlink,
                        "test")));
    }
    private static FacialAnimationDefinition.Surface surface(String name, double value) {
        return new FacialAnimationDefinition.Surface(name, Optional.empty(),
                phase(0.0D, value),
                phase(value, value),
                phase(value, 0.0D));
    }
    private static FacialAnimationDefinition.Phase phase(double from, double to) {
        return new FacialAnimationDefinition.Phase(0.2D, AnimationEndBehavior.HOLD, List.of(
                new BoneTrack(PUPIL, List.of(
                        new Keyframe(0.0D, moveX(from), InterpolationKind.LINEAR),
                        new Keyframe(0.2D, moveX(to), InterpolationKind.LINEAR)))));
    }
    private static FaceRigDefinition rig(FaceRigId id) {
        return new FaceRigDefinition(
                id,
                new BoneHierarchy(List.of(BoneDefinition.root(PUPIL))),
                List.of(),
                List.of(),
                "test");
    }
    private static Transform3 moveX(double value) {
        return new Transform3(new Vec3(value, 0.0D, 0.0D), 0.0D, Vec3.ONE, Vec3.ZERO);
    }
}
