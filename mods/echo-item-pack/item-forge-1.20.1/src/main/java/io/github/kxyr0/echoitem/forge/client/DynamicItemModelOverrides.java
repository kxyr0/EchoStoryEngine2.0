package io.github.kxyr0.echoitem.forge.client;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.core.catalog.WeaponDefinitionCatalog;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;

/**
 * Bakes authored hand models beside the normal item icon models.
 */
public final class DynamicItemModelOverrides {
    private static final String INVENTORY = "inventory";
    private final WeaponDefinitionCatalog catalog;

    public DynamicItemModelOverrides(WeaponDefinitionCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        catalog.snapshot().weapons().values().stream()
                .map(definition -> definition.visual().model())
                .flatMap(Optional::stream)
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .forEach(event::register);
    }

    public void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        catalog.snapshot().weapons().values().forEach(definition -> wrapItemModel(models, definition));
    }

    private void wrapItemModel(Map<ResourceLocation, BakedModel> models, WeaponDefinition definition) {
        Optional<ResourceLocation> handModelId = definition.visual().model()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull);
        if (handModelId.isEmpty()) {
            return;
        }
        ResourceLocation itemModelId = itemModelId(definition.id());
        BakedModel iconModel = models.get(itemModelId);
        BakedModel handModel = findModel(models, handModelId.orElseThrow());
        if (iconModel != null && handModel != null && iconModel != handModel) {
            models.put(itemModelId, new HandheldOverrideBakedModel(iconModel, handModel));
        }
    }

    private static BakedModel findModel(Map<ResourceLocation, BakedModel> models, ResourceLocation modelId) {
        BakedModel model = models.get(modelId);
        if (model != null) {
            return model;
        }
        return models.get(new ModelResourceLocation(modelId, INVENTORY));
    }

    private static ResourceLocation itemModelId(EchoItemId id) {
        return new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path()), INVENTORY);
    }
}
