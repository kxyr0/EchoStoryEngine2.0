package io.github.kxyr0.echoanimation.forge.client.dialogue;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.client.rendering.LayerDrawCommand;
import io.github.kxyr0.echoanimation.client.rendering.LayerDrawPlanner;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.core.transform.TransformEvaluator;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.forge.client.portrait.PortraitLayerConfig;
import io.github.kxyr0.echoanimation.forge.client.portrait.PortraitOverlayConfig;
import io.github.kxyr0.echoanimation.forge.client.portrait.PortraitOverlayConfigSource;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import io.github.kxyr0.echoanimation.forge.client.FaceLayerTextureCache;
import io.github.kxyr0.echoanimation.forge.client.FaceLayerTextureLocations;
import io.github.kxyr0.echoanimation.forge.client.IdleBlinkPose;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * Draws Echo Animation face layers over Echo Dialogue speaker visuals.
 */
final class DialogueVisualFaceLayerRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BASE_PLAYER_FACE_RIG = "echo_content_player:player/base";
    private static final String ECHO_WARDROBE_JOHN_PREFIX = "jdh:echo_wardrobe/skins/combo/john_";

    private final AnimationCatalogState catalogState;
    private final FaceLayerTextureCache textureCache;
    private final LayerDrawPlanner planner = new LayerDrawPlanner();
    private final TransformEvaluator evaluator = new TransformEvaluator();
    private final PortraitOverlayConfigSource overlayConfigs = new PortraitOverlayConfigSource();
    private final IdleBlinkPose idleBlinkPose = new IdleBlinkPose();
    private final DialoguePortraitAnimationState animationState;
    private final Set<String> reportedMissingRigs = ConcurrentHashMap.newKeySet();
    private final Set<String> reportedRenderedRigs = ConcurrentHashMap.newKeySet();

    DialogueVisualFaceLayerRenderer(AnimationCatalogState catalogState, FaceLayerTextureCache textureCache) {
        this(catalogState, textureCache, DialoguePortraitAnimationState.shared());
    }

    DialogueVisualFaceLayerRenderer(AnimationCatalogState catalogState, FaceLayerTextureCache textureCache,
            DialoguePortraitAnimationState animationState) {
        this.catalogState = catalogState;
        this.textureCache = textureCache;
        this.animationState = animationState;
    }

    void render(GuiGraphics graphics, String visualId, Bounds bounds) {
        String rigVisualId = rigVisualId(visualId);
        Projection projection = new Projection(bounds, overlayConfigs.config(rigVisualId));
        Optional<FaceRigDefinition> rig = find(rigVisualId);
        if (rig.isEmpty()) {
            if (reportedMissingRigs.add(visualId)) {
                LOGGER.warn("Echo Dialogue portrait face rig missing for visual={}", visualId);
            }
            return;
        }
        if (reportedRenderedRigs.add(visualId)) {
            LOGGER.info("Echo Dialogue portrait face layer rendering visual={} rig={}",
                    visualId, rig.orElseThrow().id());
        }
        renderRig(graphics, rig.orElseThrow(), projection, visualId);
    }

    static String rigVisualId(String visualId) {
        return visualId != null && visualId.startsWith(ECHO_WARDROBE_JOHN_PREFIX)
                ? BASE_PLAYER_FACE_RIG : visualId;
    }

    private void renderRig(GuiGraphics graphics, FaceRigDefinition rig, Projection projection, String visualId) {
        double sampleTick = sampleTick();
        BonePose basePose = animationState.posePortrait(catalogState.current(), rig, visualId, sampleTick)
                .orElseGet(() -> new BonePose(Map.of()));
        BonePose pose = animationState.suppressesIdleBlinkPortrait(catalogState.current(), rig, visualId)
                ? basePose
                : idleBlinkPose.apply(projection.config().controls(rig.controls()), basePose, visualId, sampleTick,
                        animationState.blinkTiming(visualId));
        var world = evaluator.evaluate(rig.hierarchy(), pose);
        var neutralWorld = evaluator.evaluate(rig.hierarchy(), new BonePose(Map.of()));
        for (LayerDrawCommand command : planner.plan(rig.layers(), Set.of())) {
            Transform3 bone = world.get(command.boneId()).orElse(Transform3.IDENTITY);
            Transform3 neutralBone = neutralWorld.get(command.boneId()).orElse(Transform3.IDENTITY);
            PortraitLayerConfig layer = projection.config().layer(command.layerId().value());
            if (layer.isVisible()) {
                draw(graphics, command, overridePlacement(combine(bone, command.placement()),
                        combine(neutralBone, command.placement()), layer), projection, layer);
            }
        }
    }

    private double sampleTick() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0.0D : level.getGameTime() + Minecraft.getInstance().getFrameTime();
    }

    private void draw(GuiGraphics graphics, LayerDrawCommand command, Transform3 placement,
            Projection projection, PortraitLayerConfig layer) {
        ResourceLocation texture = textureLocation(command.textureId().value());
        if (texture == null) {
            return;
        }
        FaceLayerTextureCache.TextureSize size = textureCache.size(texture);
        IntRect rect = command.atlasRect();
        int drawWidth = layer.width(rect.width());
        int drawHeight = layer.height(rect.height());
        PoseStack poses = graphics.pose();
        poses.pushPose();
        translateToLayerCenter(poses, placement, projection, layer);
        poses.mulPose(Axis.ZP.rotationDegrees((float) placement.rotationZDegrees()));
        poses.scale((float) (projection.scaleX() * placement.scale().x()),
                (float) (projection.scaleY() * placement.scale().y()), 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            graphics.blit(texture, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight,
                    rect.x(), rect.y(), rect.width(), rect.height(), size.width(), size.height());
        } finally {
            RenderSystem.disableBlend();
        }
        poses.popPose();
    }

    private void translateToLayerCenter(PoseStack poses, Transform3 placement, Projection projection,
            PortraitLayerConfig layer) {
        double centerX = projection.centerX()
                + placement.translation().x() * 16.0D * projection.scaleX() + layer.resolvedOffsetX();
        double centerY = projection.centerY()
                + placement.translation().y() * 16.0D * projection.scaleY() + layer.resolvedOffsetY();
        poses.translate((float) centerX, (float) centerY, 0.0F);
    }

    private ResourceLocation textureLocation(String value) {
        return FaceLayerTextureLocations.clientTexture(value);
    }

    private Optional<FaceRigDefinition> find(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            FaceRigId id = FaceRigId.parse(value);
            return catalogState.current().faceRigs().stream().filter(rig -> rig.id().equals(id)).findFirst();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Transform3 combine(Transform3 bone, Transform3 layer) {
        return new Transform3(
                bone.translation().plus(layer.translation()),
                bone.rotationZDegrees() + layer.rotationZDegrees(),
                bone.scale().componentTimes(layer.scale()),
                layer.pivot());
    }

    static Transform3 overridePlacement(Transform3 placement, Transform3 neutralPlacement,
            PortraitLayerConfig layer) {
        Vec3 translation = placement.translation();
        Vec3 neutralTranslation = neutralPlacement.translation();
        Vec3 scale = placement.scale();
        double x = layer.placeX(neutralTranslation.x()) + translation.x() - neutralTranslation.x();
        double y = layer.placeY(neutralTranslation.y()) + translation.y() - neutralTranslation.y();
        return new Transform3(
                new Vec3(x, y, translation.z()),
                placement.rotationZDegrees(),
                new Vec3(scale.x() * layer.resolvedScaleX(), scale.y() * layer.resolvedScaleY(), scale.z()),
                placement.pivot());
    }

    record Bounds(int x, int y, int width, int height) {
    }

    private record Projection(Bounds bounds, PortraitOverlayConfig config) {
        double centerX() { return bounds.x() + bounds.width() * config.centerX() + config.offsetX(); }

        double centerY() { return bounds.y() + bounds.height() * config.centerY() + config.offsetY(); }

        double scaleX() { return bounds.width() / config.facePixels(); }

        double scaleY() { return bounds.height() / config.facePixels(); }
    }
}
