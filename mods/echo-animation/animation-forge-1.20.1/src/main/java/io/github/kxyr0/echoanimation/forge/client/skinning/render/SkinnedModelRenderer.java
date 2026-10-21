package io.github.kxyr0.echoanimation.forge.client.skinning.render;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel;
import io.github.kxyr0.echoanimation.forge.client.skinning.pose.SkinningPalette;
import io.github.kxyr0.echoanimation.forge.client.skinning.render.attachment.SkinnedAttachmentTransformProvider;
import io.github.kxyr0.echoanimation.forge.debug.SkinningDebugState;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.eventbus.api.IEventBus;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
/** Selects the GPU path, exposes debug fallbacks and owns reload-sensitive caches. */
public final class SkinnedModelRenderer extends SimplePreparableReloadListener<Void> {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final int MAX_RUNTIME_BONES = 512;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CPU_PROPERTY = "echo.animation.cpuSkinning";
    private static final String BIND_PROPERTY = "echo.animation.freezeBindPose";
    private static final String DEBUG_PROPERTY = "echo.animation.skinDebug";
    private final SkinnedModelRepository models = new SkinnedModelRepository();
    private final SkinningPalette palette = new SkinningPalette();
    private final SkinningBackend gpu = new GpuSkinningBackend();
    private final CpuSkinningBackend cpu = new CpuSkinningBackend();
    private boolean weightedPreviewLogged;
    private boolean theRigBindDebugLogged;
    public static void registerShader(IEventBus modBus) {
        SkinningShader.register(modBus);
    }
    public boolean render( AnimationHumanoidRenderContext context, AnimationRigDefinition rig, AnimationPose pose,
            Runnable faceRenderer) {
        Optional<SkinnedModel> resolved = models.find(rig.id(), context.skinVariant(), MAX_RUNTIME_BONES);
        if (resolved.isEmpty()) {
            return false;
        }
        SkinnedModel model = resolved.orElseThrow();
        requireRigBones(model, rig);
        boolean debugPreview = SkinningDebugState.targets(context.subjectId());
        boolean freezeBindPose = !debugPreview && Boolean.getBoolean(BIND_PROPERTY);
        reportBindDebug(context, model);
        context.poses().pushPose();
        try {
            if (debugPreview) {
                reportWeightedPreview();
                SkinningBackend backend = requiresCpu(model) ? cpu : gpu;
                backend.render(model, palette.calculate(model, pose, false), context, 0);
            } else {
                Matrix4f[] matrices = palette.calculate(model, pose, freezeBindPose);
                SkinningBackend backend = requiresCpu(model) ? cpu : gpu;
                backend.render(model, matrices, context, debugMode());
            }
            if (!isTheRigBindDebug(context)) {
                renderBodyAttachments(context, model, pose, freezeBindPose);
            }
            if (!isTheRigDebug(context)) {
                renderHeadAttachment(context, model, pose, freezeBindPose, faceRenderer);
            }
        } finally {
            context.poses().popPose();
        }
        return true;
    }
    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }
    @Override
    protected void apply(Void value, ResourceManager resourceManager, ProfilerFiller profiler) {
        models.clear();
        gpu.clear();
        cpu.clear();
        weightedPreviewLogged = false;
        theRigBindDebugLogged = false;
    }
    private void reportBindDebug(AnimationHumanoidRenderContext context, SkinnedModel model) {
        if (isTheRigBindDebug(context) && !theRigBindDebugLogged) {
            LOGGER.warn( "[THERIG_BIND] source={} bones={} meshes={} renderOffset=({}, {}, {})"
                            + " backend={} shaderBoneLimit={}", model.source(), model.bones().size(),
                    model.meshes().size(), model.renderOffsetX(), model.renderOffsetY(), model.renderOffsetZ(),
                    requiresCpu(model) ? "CPU" : "GPU", SkinningShader.boneLimit());
            theRigBindDebugLogged = true;
        }
    }
    private void requireRigBones(SkinnedModel model, AnimationRigDefinition rig) {
        Set<String> rigBones = new HashSet<>();
        rig.bones().forEach(bone -> rigBones.add(bone.id().value()));
        model.bones() .forEach( bone -> {
                            if (!rigBones.contains(bone.name())) {
                                throw new IllegalArgumentException( "Skinned model " + model.source()
                                                + " references missing rig bone " + bone.name());
                            }
                        });
    }
    private boolean requiresCpu(SkinnedModel model) {
        return Boolean.getBoolean(CPU_PROPERTY) || model.bones().size() > SkinningShader.boneLimit();
    }
    private int debugMode() {
        String mode = System.getProperty(DEBUG_PROPERTY, "none").toLowerCase(Locale.ROOT);
        return mode.equals("dominant") || mode.equals("bone_indices") ? 1 : 0;
    }
    private void renderBodyAttachments( AnimationHumanoidRenderContext context, SkinnedModel model, AnimationPose pose,
            boolean freezeBindPose) {
        context.attachments() .render( new SkinnedAttachmentTransformProvider( palette, model, pose, freezeBindPose));
    }
    private void renderHeadAttachment( AnimationHumanoidRenderContext context, SkinnedModel model, AnimationPose pose,
            boolean freezeBindPose, Runnable renderer) {
        Matrix4f head = palette.global(model, pose, "head", freezeBindPose);
        context.poses().pushPose();
        try {
            context.poses().mulPoseMatrix(toBlockSpace(head));
            renderer.run();
        } finally {
            context.poses().popPose();
        }
    }
    private boolean isTheRigBindDebug(AnimationHumanoidRenderContext context) {
        return SkinningDebugState.targets(context.subjectId())
                && SkinningDebugState.mode() == SkinningDebugState.Mode.THERIG_BIND
                && !SkinningDebugState.therigAttachmentsEnabled();
    }
    private boolean isTheRigDebug(AnimationHumanoidRenderContext context) {
        return SkinningDebugState.targets(context.subjectId())
                && SkinningDebugState.mode() == SkinningDebugState.Mode.THERIG_BIND;
    }
    private Matrix4f toBlockSpace(Matrix4f bone) {
        return new Matrix4f().scale(PIXEL).mul(bone).scale(1.0F / PIXEL);
    }
    private void reportWeightedPreview() {
        if (weightedPreviewLogged) {
            return;
        }
        LOGGER.info( "Smooth-skinning final test: " + "exported weights with " + "full-humanoid joint animation");
        weightedPreviewLogged = true;
    }
}
