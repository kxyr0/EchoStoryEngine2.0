package io.github.kxyr0.echoanimation.forge.client;

import io.github.kxyr0.echoanimation.forge.client.dialogue.DialoguePortraitAnimationState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.client.rendering.FaceLayerPoseProjector;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationClientService;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationFaceRenderContext;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationFaceRenderResult;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderResult;
import io.github.kxyr0.echoanimation.forge.client.debug.FaceDebugPointRenderer;
import io.github.kxyr0.echoanimation.forge.client.humanoid.HumanoidRenderDispatcher;
import io.github.kxyr0.echoanimation.forge.client.skinning.render.SkinnedModelRenderer;
import io.github.kxyr0.echoanimation.forge.client.sync.AnimationClientSyncState;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.slf4j.Logger;

/**
 * Public client service implementation for authored face-layer presentation.
 */
final class AnimationClientFaceRenderService implements AnimationClientService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> REPORTED_DIALOGUE_FACE_POSES = ConcurrentHashMap.newKeySet();

    private final AnimationCatalogState catalogState; private final FaceLayerQuadRenderer quadRenderer;
    private final FaceLayerPoseProjector faceProjector = new FaceLayerPoseProjector();
    private final FaceDebugPointRenderer debugPointRenderer = new FaceDebugPointRenderer();
    private final IdleBlinkPose idleBlinkPose = new IdleBlinkPose();
    private final HumanoidRenderDispatcher humanoidRenderer;
    private final DialoguePortraitAnimationState dialogueFaceState = DialoguePortraitAnimationState.shared();

    AnimationClientFaceRenderService(AnimationCatalogState catalogState, FaceLayerQuadRenderer quadRenderer) {
        this.catalogState = catalogState; this.quadRenderer = quadRenderer;
        this.humanoidRenderer = new HumanoidRenderDispatcher(catalogState);
    }

    @Override
    public AnimationFaceRenderResult renderFace(AnimationFaceRenderContext context) {
        Optional<FaceRigDefinition> rig = resolveRig(context.rigCandidates(), context.subjectId());
        if (rig.isEmpty()) {
            return AnimationFaceRenderResult.NO_RIG;
        }
        context.poses().pushPose();
        context.head().translateAndRotate(context.poses());
        FaceRigDefinition definition = rig.orElseThrow();
        renderLayers(context.poses(), context.buffers(), context.packedLight(),
                definition, facePose(context.subjectId(), definition, context.partialTick()));
        context.poses().popPose();
        return AnimationFaceRenderResult.RENDERED;
    }

    @Override
    public AnimationHumanoidRenderResult renderHumanoid(AnimationHumanoidRenderContext context) {
        return humanoidRenderer.render(context, () -> renderFaceAtHeadOrigin(context));
    }

    SkinnedModelRenderer skinnedModelRenderer() { return humanoidRenderer.skinnedModelRenderer(); }

    private void renderFaceAtHeadOrigin(AnimationHumanoidRenderContext context) {
        Optional<FaceRigDefinition> rig = resolveRig(context.faceRigCandidates(), context.subjectId());
        if (rig.isEmpty()) {
            return;
        }
        FaceRigDefinition definition = rig.orElseThrow();
        renderLayers(context.poses(), context.buffers(), context.packedLight(),
                definition, facePose(context.subjectId(), definition, context.partialTick()));
    }

    private Optional<FaceRigDefinition> resolveRig(List<FaceRigId> candidates) {
        for (FaceRigId candidate : candidates) {
            Optional<FaceRigDefinition> rig = findFaceRig(candidate);
            if (rig.isPresent()) {
                return rig;
            }
        }
        return Optional.empty();
    }

    private Optional<FaceRigDefinition> resolveRig(List<FaceRigId> candidates, AnimationSubjectId subjectId) {
        Optional<FaceRigId> subjectRig = faceRigId(subjectId);
        if (subjectRig.isPresent() && candidates.contains(subjectRig.orElseThrow())) {
            FaceRigId rigId = subjectRig.orElseThrow();
            Optional<FaceRigDefinition> rig = findFaceRig(rigId);
            if (rig.isPresent()) {
                return rig;
            }
        }
        return resolveRig(candidates);
    }

    private Optional<FaceRigDefinition> findFaceRig(FaceRigId rigId) {
        return catalogState.current().faceRigs().stream().filter(definition -> definition.id().equals(rigId))
                .findFirst();
    }

    private Optional<FaceRigId> faceRigId(AnimationSubjectId subjectId) {
        try { return Optional.of(FaceRigId.parse(subjectId.value())); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
    }

    private long gameTick() { return Minecraft.getInstance().level == null
            ? 0L : Minecraft.getInstance().level.getGameTime(); }

    private void renderLayers(
            PoseStack poses,
            MultiBufferSource buffers,
            int packedLight,
            FaceRigDefinition rig,
            BonePose pose
    ) {
        for (var projected : faceProjector.project(rig, pose, Set.of())) {
            quadRenderer.render(poses, buffers, packedLight, projected.command(), projected.transform());
        }
        debugPointRenderer.render(poses, buffers, packedLight, rig, pose);
    }

    private BonePose facePose(AnimationSubjectId subjectId, FaceRigDefinition faceRig, float partialTick) {
        long tick = gameTick();
        double sampleTick = tick + partialTick;
        String subject = subjectId.toString();
        AnimationRigDefinition poseRig = facePoseRig(faceRig);
        BonePose pose = AnimationClientSyncState.resolvePose(subjectId, poseRig,
                catalogState.current().animationClips(), sampleTick)
                .map(this::bonePose)
                .orElseGet(() -> new BonePose(Map.of()));
        Optional<BonePose> dialogueExtra = dialogueFaceState.poseSubject(catalogState.current(), faceRig, subject,
                sampleTick);
        dialogueExtra.ifPresent(extra -> reportDialoguePose(subject, faceRig, extra));
        BonePose dialoguePose = dialogueExtra.map(extra -> merge(pose, extra)).orElse(pose);
        if (dialogueFaceState.suppressesIdleBlinkSubject(catalogState.current(), faceRig, subject)) {
            return dialoguePose;
        }
        return idleBlinkPose.apply(faceRig.controls(), dialoguePose, subject, sampleTick,
                dialogueFaceState.blinkTiming(subject));
    }

    private void reportDialoguePose(String subject, FaceRigDefinition faceRig, BonePose pose) {
        String key = subject + "|" + faceRig.id();
        if (REPORTED_DIALOGUE_FACE_POSES.add(key)) {
            LOGGER.info("Echo dialogue 3D face pose sampled subject={} rig={} bones={}",
                    subject, faceRig.id(), pose.localTransforms().keySet());
        }
    }

    private BonePose merge(BonePose base, BonePose extra) {
        Map<BoneId, Transform3> transforms = new LinkedHashMap<>(base.localTransforms());
        extra.localTransforms().forEach((bone, transform) -> transforms.merge(bone, transform, Transform3::plusDelta));
        return new BonePose(transforms);
    }

    private AnimationRigDefinition facePoseRig(FaceRigDefinition faceRig) {
        List<AnimationBoneDefinition> bones = new ArrayList<>();
        faceRig.hierarchy().bones().forEach(bone -> bones.add(new AnimationBoneDefinition(
                bone.id(),
                bone.parentId(),
                euler(Transform3.IDENTITY),
                Optional.empty())));
        return new AnimationRigDefinition(AnimationRigId.parse(faceRig.id().value()), 1L, AnimationRigKind.CUSTOM,
                AnimationSkinVariant.CLASSIC, bones, List.of(), List.of(), faceRig.semanticFingerprint());
    }

    private BonePose bonePose(AnimationPose pose) {
        Map<BoneId, Transform3> transforms = new LinkedHashMap<>();
        pose.transforms().forEach((boneId, transform) -> transforms.put(boneId, transform3(transform)));
        return new BonePose(transforms);
    }

    private EulerTransform euler(Transform3 transform) {
        return new EulerTransform(transform.translation(),
                new Vec3(0.0D, 0.0D, transform.rotationZDegrees()), transform.scale(), transform.pivot());
    }

    private Transform3 transform3(EulerTransform transform) {
        return new Transform3(transform.translation(), transform.rotationDegrees().z(),
                transform.scale(), transform.pivot());
    }
}
