package io.github.kxyr0.echoitemattachments.forge.api.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class ItemGripCatalog implements ResourceManagerReloadListener {
    public static final ItemGripCatalog INSTANCE = new ItemGripCatalog();
    private volatile List<ItemGripProfile> profiles = List.of(defaultProfile());

    private ItemGripCatalog() {
    }

    public void apply(PoseStack poses, AttachmentPointId point, net.minecraft.world.item.ItemStack stack) {
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        ItemGripProfile profile = profiles.stream().filter(value -> value.itemId().map(itemId::equals).orElse(false))
                .findFirst().orElseGet(() -> profiles.stream().filter(value -> value.itemId().isEmpty())
                        .findFirst().orElse(defaultProfile()));
        ItemGripTransform value = profile.transforms().getOrDefault(point, defaults().get(point));
        poses.translate(value.x(), value.y(), value.z());
        poses.mulPose(Axis.XP.rotationDegrees(value.rotateX()));
        poses.mulPose(Axis.YP.rotationDegrees(value.rotateY()));
        poses.mulPose(Axis.ZP.rotationDegrees(value.rotateZ()));
        poses.scale(value.scale(), value.scale(), value.scale());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        List<ItemGripProfile> loaded = new ArrayList<>();
        try {
            var found = resources.listResources("grips", location -> location.getPath().endsWith(".json"));
            for (var entry : found.entrySet()) {
                try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    loaded.add(new ItemGripProfile(root.has("item")
                            ? java.util.Optional.of(root.get("item").getAsString()) : java.util.Optional.empty(),
                            Map.of(AttachmentPointId.LEFT_HAND, read(root.getAsJsonObject("left_hand")),
                                    AttachmentPointId.RIGHT_HAND, read(root.getAsJsonObject("right_hand")))));
                }
            }
            loaded.sort(Comparator.comparing(profile -> profile.itemId().isEmpty()));
            if (loaded.stream().noneMatch(profile -> profile.itemId().isEmpty())) loaded.add(defaultProfile());
            profiles = List.copyOf(loaded);
        } catch (RuntimeException | java.io.IOException exception) {
            profiles = List.of(defaultProfile());
        }
    }

    private ItemGripTransform read(JsonObject value) {
        var position = value.getAsJsonArray("translate");
        var rotation = value.getAsJsonArray("rotate_degrees");
        return new ItemGripTransform(position.get(0).getAsDouble(), position.get(1).getAsDouble(),
                position.get(2).getAsDouble(), rotation.get(0).getAsFloat(), rotation.get(1).getAsFloat(),
                rotation.get(2).getAsFloat(), value.get("scale").getAsFloat());
    }

    private static Map<AttachmentPointId, ItemGripTransform> defaults() {
        ItemGripTransform value = new ItemGripTransform(0.0D, 0.62D, 0.0D,
                -90.0F, 0.0F, 0.0F, 1.0F);
        return Map.of(AttachmentPointId.LEFT_HAND, value, AttachmentPointId.RIGHT_HAND, value);
    }

    private static ItemGripProfile defaultProfile() {
        return new ItemGripProfile(java.util.Optional.empty(), defaults());
    }
}
