package io.github.kxyr0.echoplayerhud.forge.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

final class PlayerHudIconResolver {
    private static final int MAX_CACHE_ENTRIES = 32;
    private final Map<String, CachedIcon> cache = new LinkedHashMap<>(16, 0.75F, true);
    private Path baseDir;

    void useBaseDir(Path baseDir) {
        this.baseDir = baseDir;
    }

    void clear() {
        cache.clear();
    }

    Optional<HudIconTexture> resolve(String raw, int fallbackWidth, int fallbackHeight) {
        if (raw.isBlank()) {
            return Optional.empty();
        }
        return externalAlias(raw)
                .or(() -> resource(raw, fallbackWidth, fallbackHeight))
                .or(() -> namespaced(raw) ? Optional.empty() : external(raw));
    }

    private Optional<HudIconTexture> externalAlias(String raw) {
        if (!namespaced(raw)) {
            return Optional.empty();
        }
        int separator = raw.indexOf(':');
        String namespace = raw.substring(0, separator);
        String path = raw.substring(separator + 1);
        if ("echo_player_hud".equals(namespace) && path.startsWith("icons/")) {
            return external(path);
        }
        return Optional.empty();
    }

    private Optional<HudIconTexture> resource(String raw, int fallbackWidth, int fallbackHeight) {
        if (!namespaced(raw)) {
            return Optional.empty();
        }
        int separator = raw.indexOf(':');
        try {
            String namespace = raw.substring(0, separator);
            String path = raw.substring(separator + 1);
            return resource(namespace, path, fallbackWidth, fallbackHeight)
                    .or(() -> path.startsWith("textures/") ? Optional.empty()
                            : resource(namespace, "textures/" + path, fallbackWidth, fallbackHeight));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<HudIconTexture> resource(String namespace, String path, int fallbackWidth, int fallbackHeight) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(namespace, path);
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new HudIconTexture(texture, Math.max(1, fallbackWidth), Math.max(1, fallbackHeight)));
    }

    private Optional<HudIconTexture> external(String raw) {
        Path path;
        try {
            path = path(raw);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
        long stamp = stamp(path);
        String key = path.toString();
        CachedIcon cached = cache.get(key);
        if (cached != null && cached.stamp() == stamp) {
            return cached.texture();
        }
        Optional<HudIconTexture> loaded = loadExternal(path, stamp);
        cache.put(key, new CachedIcon(stamp, loaded));
        trim();
        return loaded;
    }

    private boolean namespaced(String raw) {
        return raw.contains(":") && !raw.matches("^[A-Za-z]:.*");
    }

    private Path path(String raw) {
        Path path = Path.of(raw);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        Path root = baseDir == null ? Path.of(".") : baseDir;
        return root.resolve(path).normalize();
    }

    private long stamp(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.getLastModifiedTime(path).toMillis() : Long.MIN_VALUE;
        } catch (IOException exception) {
            return Long.MIN_VALUE;
        }
    }

    private Optional<HudIconTexture> loadExternal(Path path, long stamp) {
        if (stamp == Long.MIN_VALUE) {
            return Optional.empty();
        }
        try (InputStream input = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(input);
            int width = image.getWidth();
            int height = image.getHeight();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("echo_player_hud",
                    "external_icons/" + Integer.toHexString(path.toString().hashCode()) + "_" + stamp);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            return Optional.of(new HudIconTexture(id, width, height));
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private void trim() {
        while (cache.size() > MAX_CACHE_ENTRIES) {
            cache.remove(cache.keySet().iterator().next());
        }
    }

    record HudIconTexture(ResourceLocation texture, int width, int height) {
    }

    private record CachedIcon(long stamp, Optional<HudIconTexture> texture) {
    }
}
