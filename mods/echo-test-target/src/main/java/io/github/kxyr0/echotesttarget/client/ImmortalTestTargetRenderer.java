package io.github.kxyr0.echotesttarget.client;

import io.github.kxyr0.echotesttarget.entity.ImmortalTestTarget;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ImmortalTestTargetRenderer
        extends MobRenderer<ImmortalTestTarget, ZombieModel<ImmortalTestTarget>> {
    private static final ResourceLocation ZOMBIE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public ImmortalTestTargetRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ImmortalTestTarget entity) {
        return ZOMBIE_TEXTURE;
    }
}
