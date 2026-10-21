package io.github.kxyr0.echodialogue.forge.client.visual;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

/**
 * Client-only bounded visual resource resolver.
 */
public final class ClientSpeakerVisualResolver {
    private static final int MAX_CACHE_ENTRIES = 64;
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<String, Optional<ClientSpeakerVisual>> cache = new LinkedHashMap<>(16, 0.75F, true);
    private final Set<String> warned = new LinkedHashSet<>();

    public Optional<ClientSpeakerVisual> resolve(Optional<DialogueOpenMessage.VisualPayload> payload) {
        return payload.flatMap(this::resolve);
    }

    public void clear() {
        cache.clear();
        warned.clear();
    }

    private Optional<ClientSpeakerVisual> resolve(DialogueOpenMessage.VisualPayload payload) {
        String key = payload.kind() + "|" + payload.id();
        Optional<ClientSpeakerVisual> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<ClientSpeakerVisual> resolved = resolveFresh(payload);
        cache.put(key, resolved);
        trimCache();
        return resolved;
    }

    private Optional<ClientSpeakerVisual> resolveFresh(DialogueOpenMessage.VisualPayload payload) {
        SpeakerVisualKind kind = SpeakerVisualKind.valueOf(payload.kind());
        SpeakerVisualId id = SpeakerVisualId.parse(payload.id());
        for (ResourceLocation texture : textureLocations(kind, id)) {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isPresent()) {
                Optional<ClientSpeakerVisual> visual = readTexture(kind, id, texture, resource.orElseThrow());
                if (visual.isPresent()) {
                    return visual;
                }
            }
        }
        warnOnce("EDLG-CLIENT-VISUAL-001", kind, id, "ресурс не найден");
        return Optional.empty();
    }

    private Optional<ClientSpeakerVisual> readTexture(
            SpeakerVisualKind kind,
            SpeakerVisualId id,
            ResourceLocation texture,
            Resource resource
    ) {
        try (InputStream stream = resource.open(); NativeImage image = NativeImage.read(stream)) {
            boolean skinSheet = skinSheetCompatible(kind, image.getWidth(), image.getHeight());
            if (kind == SpeakerVisualKind.SKIN_HEAD && !skinSheet) {
                warnOnce("EDLG-CLIENT-VISUAL-002", kind, id, "поддерживается только skin 64x64");
                return Optional.empty();
            }
            if (skinSheet) {
                return Optional.of(ClientSpeakerVisual.skinSheet(kind, id, texture,
                        image.getWidth(), image.getHeight()));
            }
            return Optional.of(ClientSpeakerVisual.image(kind, id, texture, image.getWidth(), image.getHeight()));
        } catch (IOException | RuntimeException exception) {
            warnOnce("EDLG-CLIENT-VISUAL-003", kind, id, "PNG повреждён или не поддерживается");
            return Optional.empty();
        }
    }

    static boolean skinSheetCompatible(SpeakerVisualKind kind, int width, int height) {
        return (kind == SpeakerVisualKind.SKIN_HEAD || kind == SpeakerVisualKind.PORTRAIT)
                && width == 64 && height == 64;
    }

    static ResourceLocation[] textureLocations(SpeakerVisualKind kind, SpeakerVisualId id) {
        if (kind == SpeakerVisualKind.SKIN_HEAD) {
            return new ResourceLocation[] {
                    dialogueTexture("skins", id),
                    ResourceLocation.fromNamespaceAndPath(id.value().namespace(), "textures/echo_npc/skins/"
                            + id.value().path() + ".png"),
                    ResourceLocation.fromNamespaceAndPath(id.value().namespace(),
                            "textures/echo_player/skins/" + id.value().path() + ".png"),
                    ResourceLocation.fromNamespaceAndPath(id.value().namespace(), "textures/"
                            + id.value().path() + ".png")
            };
        }
        return new ResourceLocation[] {dialogueTexture("portraits", id)};
    }

    private static ResourceLocation dialogueTexture(String root, SpeakerVisualId id) {
        return ResourceLocation.fromNamespaceAndPath(id.value().namespace(), "textures/echo_dialogue/"
                + root + "/" + id.value().path() + ".png");
    }

    private void trimCache() {
        while (cache.size() > MAX_CACHE_ENTRIES) {
            String first = cache.keySet().iterator().next();
            cache.remove(first);
        }
    }

    private void warnOnce(String code, SpeakerVisualKind kind, SpeakerVisualId id, String message) {
        String key = code + "|" + kind.name() + "|" + id;
        if (warned.add(key)) {
            LOGGER.warn("{} {} {}: {}", code, kind.name(), id, message);
        }
        while (warned.size() > MAX_CACHE_ENTRIES) {
            warned.remove(warned.iterator().next());
        }
    }
}
