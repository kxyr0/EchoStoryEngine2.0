package io.github.kxyr0.echoitem.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps the regular item icon model in GUI slots while using a separate model when equipped.
 */
final class HandheldOverrideBakedModel implements BakedModel {
    private final BakedModel iconModel;
    private final BakedModel handModel;

    HandheldOverrideBakedModel(BakedModel iconModel, BakedModel handModel) {
        this.iconModel = Objects.requireNonNull(iconModel, "iconModel");
        this.handModel = Objects.requireNonNull(handModel, "handModel");
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource random) {
        return iconModel.getQuads(state, direction, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return iconModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return iconModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return iconModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return iconModel.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return iconModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return iconModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return iconModel.getOverrides();
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
        BakedModel selected = equippedContext(context) ? handModel : iconModel;
        BakedModel transformed = selected.applyTransform(context, poseStack, leftHand);
        return translucentHeadContext(context) ? new TranslucentHeadBakedModel(transformed) : transformed;
    }

    static boolean translucentHeadContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.HEAD;
    }

    private static boolean equippedContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.HEAD;
    }
}
