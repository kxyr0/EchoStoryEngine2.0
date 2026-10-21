package io.github.kxyr0.echoitem.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class HandheldOverrideBakedModelTest {
    @Test
    void headContextForcesTranslucentModelWithoutChangingOtherContexts() {
        BakedModel icon = new StubModel();
        BakedModel equipped = new StubModel();
        HandheldOverrideBakedModel model = new HandheldOverrideBakedModel(icon, equipped);

        BakedModel head = model.applyTransform(ItemDisplayContext.HEAD, new PoseStack(), false);
        BakedModel hand = model.applyTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, new PoseStack(), false);
        BakedModel gui = model.applyTransform(ItemDisplayContext.GUI, new PoseStack(), false);

        Assertions.assertInstanceOf(TranslucentHeadBakedModel.class, head);
        Assertions.assertSame(equipped, hand);
        Assertions.assertSame(icon, gui);
    }

    private static final class StubModel implements BakedModel {
        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource random) {
            return List.of();
        }

        @Override public boolean useAmbientOcclusion() { return false; }

        @Override public boolean isGui3d() { return true; }

        @Override public boolean usesBlockLight() { return true; }

        @Override public boolean isCustomRenderer() { return false; }

        @Override public TextureAtlasSprite getParticleIcon() { return null; }

        @Override public ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }

        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }
}
