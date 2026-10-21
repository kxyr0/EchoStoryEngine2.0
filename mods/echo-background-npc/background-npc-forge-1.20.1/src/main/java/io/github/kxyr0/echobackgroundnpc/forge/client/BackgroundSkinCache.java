package io.github.kxyr0.echobackgroundnpc.forge.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Shared immutable texture resolution; a skin is never decoded per NPC. */
public final class BackgroundSkinCache {
    private static final ResourceLocation FALLBACK = new ResourceLocation("minecraft",
            "textures/entity/player/wide/steve.png");
    private final Map<String, ResourceLocation> cache = new HashMap<>();
    private final Set<String> missing = new HashSet<>();

    public ResourceLocation resolve(String appearance) {
        return cache.computeIfAbsent(appearance, this::find);
    }
    public void clear() { cache.clear(); missing.clear(); }

    private ResourceLocation find(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || cache.size() >= 256) return FALLBACK;
        ResourceLocation texture = new ResourceLocation(id.getNamespace(), "textures/entity/background_npc/"
                + id.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) return texture;
        missing.add(value);
        return FALLBACK;
    }
}
