package io.github.kxyr0.echonpc.forge.client;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.forge.EchoNpcMod;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * Client lifecycle-owned bounded skin resource resolver.
 */
public final class SkinResourceCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CACHE = 256;
    private static final ResourceLocation FALLBACK =
            new ResourceLocation(EchoNpcMod.MOD_ID, "textures/echo_npc/skins/fallback.png");
    private final Map<String, ResourceLocation> cache = new HashMap<>();
    private final Set<String> missing = new HashSet<>();

    public ResourceLocation location(String skinId) {
        if (skinId == null || skinId.isBlank()) {
            return FALLBACK;
        }
        return cache.computeIfAbsent(skinId, this::resolve);
    }

    public void clear() {
        cache.clear();
        missing.clear();
    }

    private ResourceLocation resolve(String skinId) {
        if (cache.size() >= MAX_CACHE) {
            return FALLBACK;
        }
        ResourceLocation id = ResourceLocation.tryParse(skinId);
        if (id == null) {
            return FALLBACK;
        }
        ResourceLocation texture = new ResourceLocation(id.getNamespace(), "textures/entity/"
                + id.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            return texture;
        }
        texture = new ResourceLocation(id.getNamespace(), "textures/echo_npc/skins/" + id.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            return texture;
        }
        logMissing(skinId);
        return FALLBACK;
    }

    private void logMissing(String skinId) {
        if (missing.size() < MAX_CACHE && missing.add(skinId)) {
            LOGGER.warn("ENPC-SKIN-MISSING skin resource отсутствует; используется fallback: {}", skinId);
        }
    }
}
