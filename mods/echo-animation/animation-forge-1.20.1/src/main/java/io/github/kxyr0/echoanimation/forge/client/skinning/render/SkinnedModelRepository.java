package io.github.kxyr0.echoanimation.forge.client.skinning.render;

import com.mojang.logging.LogUtils;

import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModelReader;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Lazy resource-pack model cache keyed by rig and detected player-skin arm width. */
final class SkinnedModelRepository {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<ModelKey, Optional<SkinnedModel>> cache = new HashMap<>();
    private final SkinnedModelReader reader = new SkinnedModelReader();

    Optional<SkinnedModel> find(AnimationRigId rigId, AnimationSkinVariant variant, int boneLimit) {
        ModelKey key = new ModelKey(rigId, variant);
        return cache.computeIfAbsent(key, ignored -> load(key, boneLimit));
    }

    void clear() {
        cache.clear();
    }

    private Optional<SkinnedModel> load(ModelKey key, int boneLimit) {
        ResourceLocation rig = ResourceLocation.parse(key.rigId().value());
        String suffix = key.variant() == AnimationSkinVariant.SLIM ? "_slim" : "";
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath( rig.getNamespace(),
                        "echo_animation/models/" + rig.getPath() + suffix + ".skin.json");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(modelId);
        if (resource.isEmpty() && key.variant() == AnimationSkinVariant.SLIM) {
            modelId = ResourceLocation.fromNamespaceAndPath( rig.getNamespace(),
                            "echo_animation/models/" + rig.getPath() + ".skin.json");
            resource = Minecraft.getInstance().getResourceManager().getResource(modelId);
        }
        if (resource.isEmpty()) {
            LOGGER.error( "Skinned rig {} variant {} has no model resource {}", key.rigId(), key.variant(), modelId);
            return Optional.empty();
        }
        try (Reader input = resource.orElseThrow().openAsReader()) {
            SkinnedModel model = reader.read(modelId.toString(), input, boneLimit);
            LOGGER.info( "Loaded skinned model {} variant={} formatVersion={} bones={} vertices={}"
                        + " renderOffset=({}, {}, {})", modelId, key.variant(), model.formatVersion(),
                    model.bones().size(), model.vertexCount(), model.renderOffsetX(), model.renderOffsetY(),
                    model.renderOffsetZ());
            return Optional.of(model);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Cannot load skinned model {}: {}", modelId, exception.getMessage());
            return Optional.empty();
        }
    }

    private record ModelKey(AnimationRigId rigId, AnimationSkinVariant variant) {}
}
