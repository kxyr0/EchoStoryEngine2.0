package io.github.kxyr0.echoteleportmagic.forge.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** RenderType owns state setup/restore, including additive blend and depth writes. */
public abstract class TeleportRenderTypes extends RenderType {
    public static final ResourceLocation ATLAS = new ResourceLocation("echo_teleport_magic", "textures/effect/atlas.png");
    public static final RenderType FIELD = create("echo_teleport_field", DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS, 262144, false, false, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(TeleportLightProgram::program)).setTextureState(new TextureStateShard(ATLAS, true, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));
    public static final RenderType BODY = create("echo_teleport_body", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 262144, false, false, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(TeleportLightProgram::program))
                    .setTextureState(new TextureStateShard(new ResourceLocation("echo_teleport_magic", "textures/effect/body.png"), false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));
    private TeleportRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
            int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sorted, setup, clear);
    }
}
