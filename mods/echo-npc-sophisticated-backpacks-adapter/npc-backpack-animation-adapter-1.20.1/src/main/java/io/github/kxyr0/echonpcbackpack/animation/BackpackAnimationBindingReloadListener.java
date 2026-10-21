package io.github.kxyr0.echonpcbackpack.animation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public final class BackpackAnimationBindingReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private final BackpackAnimationBindingCatalog catalog;

    public BackpackAnimationBindingReloadListener(BackpackAnimationBindingCatalog catalog) {
        super(GSON, "echo_npc_sophisticated_backpacks/animation_bindings");
        this.catalog = catalog;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager,
            ProfilerFiller profiler) {
        List<NpcBackpackAnimationBinding> loaded = new ArrayList<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            JsonObject value = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey().toString());
            loaded.add(new NpcBackpackAnimationBinding(
                    new ActorId(GsonHelper.getAsString(value, "npc")),
                    GsonHelper.getAsString(value, "form", "*"),
                    AnimationId.parse(GsonHelper.getAsString(value, "equip_animation")),
                    AnimationId.parse(GsonHelper.getAsString(value, "unequip_animation"))));
        });
        long uniqueKeys = loaded.stream().map(binding -> binding.actorId() + "|" + binding.formId())
                .distinct().count();
        if (uniqueKeys != loaded.size()) throw new IllegalArgumentException(
                "Duplicate NPC backpack animation binding for the same actor and form.");
        catalog.install(loaded);
    }
}
