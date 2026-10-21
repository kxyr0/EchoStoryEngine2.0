package io.github.kxyr0.echoteleportmagic.forge.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Smooth alpha has no vanilla 0.1 cutoff and uses an explicit additive shader blend. */
@Mod.EventBusSubscriber(modid = "echo_teleport_magic", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TeleportLightProgram {
    private static final AtomicReference<ShaderInstance> PROGRAM = new AtomicReference<>();
    private TeleportLightProgram() { }
    public static ShaderInstance program() { return PROGRAM.get(); }
    @SubscribeEvent public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation("echo_teleport_magic", "soft_light"), DefaultVertexFormat.POSITION_COLOR_TEX), PROGRAM::set);
    }
}
