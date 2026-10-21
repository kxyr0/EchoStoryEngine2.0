package io.github.kxyr0.echomagic.forge.client;

import io.github.kxyr0.echomagic.forge.EchoMagicMod;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class MagicProjectileRenderer extends EntityRenderer<MagicProjectileEntity> {
    private static final ResourceLocation EMPTY = new ResourceLocation(EchoMagicMod.MOD_ID, "textures/empty.png");
    public MagicProjectileRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(MagicProjectileEntity entity) { return EMPTY; }
}
