package io.github.kxyr0.echonpcbackpack.forge.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

public final class BackpackTransformCatalog extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private volatile Map<String, Map<NpcBackpackAttachmentPoint, BackpackAttachmentTransform>> transforms = defaults();

    public BackpackTransformCatalog() {
        super(GSON, "attachment_transforms");
    }

    public BackpackAttachmentTransform resolve(String form, NpcBackpackAttachmentPoint point) {
        Map<NpcBackpackAttachmentPoint, BackpackAttachmentTransform> values =
                transforms.getOrDefault(form, transforms.get("*"));
        return values.getOrDefault(point, transforms.get("*").get(point));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager,
            ProfilerFiller profiler) {
        Map<String, Map<NpcBackpackAttachmentPoint, BackpackAttachmentTransform>> loaded = defaults();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            JsonObject root = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey().toString());
            String form = GsonHelper.getAsString(root, "form", "*");
            EnumMap<NpcBackpackAttachmentPoint, BackpackAttachmentTransform> points =
                    new EnumMap<>(NpcBackpackAttachmentPoint.class);
            read(root, "back", NpcBackpackAttachmentPoint.BACK, points);
            read(root, "left_hand", NpcBackpackAttachmentPoint.LEFT_HAND, points);
            read(root, "right_hand", NpcBackpackAttachmentPoint.RIGHT_HAND, points);
            loaded.put(form, Map.copyOf(points));
        });
        transforms = Map.copyOf(loaded);
    }

    private void read(JsonObject root, String key, NpcBackpackAttachmentPoint point,
            Map<NpcBackpackAttachmentPoint, BackpackAttachmentTransform> target) {
        if (!root.has(key)) return;
        JsonObject value = GsonHelper.getAsJsonObject(root, key);
        target.put(point, new BackpackAttachmentTransform(number(value, "x", 0.0F),
                number(value, "y", 0.0F), number(value, "z", 0.0F),
                number(value, "rotate_x", 0.0F), number(value, "rotate_y", 0.0F),
                number(value, "rotate_z", 0.0F), number(value, "scale", 1.0F)));
    }

    private float number(JsonObject root, String key, float fallback) {
        return root.has(key) ? root.get(key).getAsFloat() : fallback;
    }

    private static Map<String, Map<NpcBackpackAttachmentPoint, BackpackAttachmentTransform>> defaults() {
        EnumMap<NpcBackpackAttachmentPoint, BackpackAttachmentTransform> points =
                new EnumMap<>(NpcBackpackAttachmentPoint.class);
        points.put(NpcBackpackAttachmentPoint.BACK,
                new BackpackAttachmentTransform(0.0F, 0.25F, 0F, 0.0F, 180.0F, 180.0F, 1.0F));
        points.put(NpcBackpackAttachmentPoint.LEFT_HAND,
                new BackpackAttachmentTransform(0.0F, 0.68F, 0.0F, -90.0F, 0.0F, 0.0F, 0.55F));
        points.put(NpcBackpackAttachmentPoint.RIGHT_HAND,
                new BackpackAttachmentTransform(0.0F, 0.68F, 0.0F, -90.0F, 0.0F, 0.0F, 0.55F));
        Map<String, Map<NpcBackpackAttachmentPoint, BackpackAttachmentTransform>> result = new LinkedHashMap<>();
        result.put("*", Map.copyOf(points));
        return result;
    }
}
