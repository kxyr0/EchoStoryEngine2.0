package io.github.kxyr0.echoanimation.forge.client.skinning.render;

import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel;
import io.github.kxyr0.echoanimation.forge.client.skinning.render.gpu.GpuSkinningMesh;
import io.github.kxyr0.echoanimation.forge.client.skinning.render.gpu.ShaderPackEntityDrawer;
import io.github.kxyr0.echoanimation.forge.client.skinning.render.gpu.SkinningTransformProgram;
import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;

/** GPU transform-feedback deformation followed by the active vanilla/Iris entity pass. */
final class GpuSkinningBackend implements SkinningBackend {
    private final Map<String, GpuSkinningMesh> meshes = new HashMap<>();
    private final SkinningTransformProgram transform = new SkinningTransformProgram();
    private final ShaderPackEntityDrawer drawer = new ShaderPackEntityDrawer();

    @Override
    public void render(
            SkinnedModel model,
            Matrix4f[] palette,
            AnimationHumanoidRenderContext context,
            int debugMode
    ) {
        GpuSkinningMesh mesh = meshes.computeIfAbsent(model.source(), ignored -> new GpuSkinningMesh(model));
        transform.deform(mesh, palette, debugMode == 2);
        drawer.draw(mesh, context);
    }

    @Override
    public void clear() {
        meshes.values().forEach(GpuSkinningMesh::close);
        meshes.clear();
        transform.close();
    }
}
