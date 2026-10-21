package io.github.kxyr0.echoanimation.forge.client.humanoid;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderResult;
import io.github.kxyr0.echoanimation.forge.client.humanoid.state.HumanoidPoseResolver;
import io.github.kxyr0.echoanimation.forge.client.humanoid.state.HumanoidRigResolver;
import io.github.kxyr0.echoanimation.forge.client.skinning.render.SkinnedModelRenderer;
import io.github.kxyr0.echoanimation.forge.debug.SkinningDebugState;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import java.util.Optional;
import net.minecraft.client.model.geom.ModelPart;
import org.slf4j.Logger;

/**
 * Production NPC humanoid dispatcher.
 *
 * <p>BODY pose ownership is resolved before rendering. Sitting is exclusive,
 * while face rendering stays a separate pass.</p>
 */
public final class HumanoidRenderDispatcher {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final SegmentedHumanoidModelRenderer segmented = new SegmentedHumanoidModelRenderer();
    private final SkinnedModelRenderer skinned = new SkinnedModelRenderer();
    private final HumanoidPoseRetargeter retargeter = new HumanoidPoseRetargeter();
    private final HumanoidRigResolver rigs;
    private final HumanoidPoseResolver poses;

    public HumanoidRenderDispatcher(AnimationCatalogState catalogState) {
        rigs = new HumanoidRigResolver(catalogState);
        poses = new HumanoidPoseResolver(catalogState);
    }

    public AnimationHumanoidRenderResult render(
            AnimationHumanoidRenderContext context,
            Runnable faceRenderer
    ) {
        if (SkinningDebugState.targets(context.subjectId())) {
            return renderSkinningDebug(context, faceRenderer);
        }
        Optional<AnimationRigDefinition> resolved = rigs.segmented(context);
        if (resolved.isEmpty()) {
            return AnimationHumanoidRenderResult.NOT_HANDLED;
        }
        AnimationRigDefinition rig = resolved.orElseThrow();
        AnimationPose pose = poses.resolve(context, rig)
                .orElseGet(() -> AnimationPose.defaults(rig.bones()));
        try {
            segmented.render(context, rig, pose);
            renderSegmentedFace(context, faceRenderer);
            return AnimationHumanoidRenderResult.REPLACED_DEFAULT_MODEL;
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Cannot render segmented humanoid subject={} rig={}",
                    context.subjectId(),
                    rig.id(),
                    exception);
            return AnimationHumanoidRenderResult.FAILED;
        }
    }

    public ModelPart head(AnimationSkinVariant variant) {
        return segmented.head(variant);
    }

    public SkinnedModelRenderer skinnedModelRenderer() {
        return skinned;
    }

    private AnimationHumanoidRenderResult renderSkinningDebug(
            AnimationHumanoidRenderContext context,
            Runnable faceRenderer
    ) {
        Optional<AnimationRigDefinition> resolved = rigs.debug();
        Optional<AnimationRigDefinition> renderRig = rigs.smoothDebug();
        if (resolved.isEmpty() || renderRig.isEmpty() || !supported(resolved.orElseThrow())) {
            return AnimationHumanoidRenderResult.NOT_HANDLED;
        }
        AnimationRigDefinition sourceRig = resolved.orElseThrow();
        AnimationRigDefinition targetRig = renderRig.orElseThrow();
        AnimationPose pose = poses.resolve(context, sourceRig)
                .orElseGet(() -> AnimationPose.defaults(sourceRig.bones()));
        try {
            AnimationPose renderPose = sourceRig.id().equals(targetRig.id())
                    ? pose
                    : retargeter.retarget(sourceRig, targetRig, pose);
            if (!skinned.render(context, targetRig, renderPose, faceRenderer)) {
                return AnimationHumanoidRenderResult.NOT_HANDLED;
            }
            return AnimationHumanoidRenderResult.REPLACED_DEFAULT_MODEL;
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Cannot render debug skinned humanoid subject={} sourceRig={} renderRig={}",
                    context.subjectId(),
                    sourceRig.id(),
                    targetRig.id(),
                    exception);
            return AnimationHumanoidRenderResult.FAILED;
        }
    }

    private void renderSegmentedFace(
            AnimationHumanoidRenderContext context,
            Runnable faceRenderer
    ) {
        context.poses().pushPose();
        try {
            segmented.applyHeadTransform(context.skinVariant(), context.poses());
            faceRenderer.run();
        } finally {
            context.poses().popPose();
        }
    }

    private boolean supported(AnimationRigDefinition rig) {
        return rig.kind() == AnimationRigKind.SEGMENTED_HUMANOID
                || rig.kind() == AnimationRigKind.SKINNED_MESH;
    }
}
