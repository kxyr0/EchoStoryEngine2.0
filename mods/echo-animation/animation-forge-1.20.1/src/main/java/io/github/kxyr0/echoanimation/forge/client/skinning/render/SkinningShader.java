package io.github.kxyr0.echoanimation.forge.client.skinning.render;

import io.github.kxyr0.echoanimation.forge.EchoAnimationMod;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** Reload-safe shader registration and hardware-derived uniform palette limit. */
final class SkinningShader {
    static final int SHADER_BONE_CAPACITY = 48;
    private static final AtomicReference<ShaderInstance> SHADER = new AtomicReference<>();
    private static final AtomicInteger BONE_LIMIT = new AtomicInteger(SHADER_BONE_CAPACITY);

    private SkinningShader() {
    }

    static void register(IEventBus bus) {
        bus.addListener(SkinningShader::onRegister);
    }

    static ShaderInstance instance() {
        ShaderInstance shader = SHADER.get();
        if (shader == null) {
            throw new IllegalStateException("Echo smooth-skinning shader is not loaded.");
        }
        return shader;
    }

    static int boneLimit() {
        return BONE_LIMIT.get();
    }

    private static void onRegister(RegisterShadersEvent event) {
        int components = GL11.glGetInteger(GL20.GL_MAX_VERTEX_UNIFORM_COMPONENTS);
        BONE_LIMIT.set(Math.max(1, Math.min(SHADER_BONE_CAPACITY, (components - 64) / 16)));
        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(EchoAnimationMod.MOD_ID, "skinned_entity"),
                    SkinnedVertexFormat.FORMAT);
            event.registerShader(shader, SHADER::set);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Echo smooth-skinning shader.", exception);
        }
    }
}
