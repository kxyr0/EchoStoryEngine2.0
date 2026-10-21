package io.github.kxyr0.echowardrobe.forge;

import io.github.kxyr0.echowardrobe.api.contribution.WardrobeAppearanceContribution;
import io.github.kxyr0.echowardrobe.api.contribution.WearableModelContribution;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantId;
import io.github.kxyr0.echowardrobe.api.outfit.ResolvedOutfit;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.runtime.EquippedWearableSnapshot;
import io.github.kxyr0.echowardrobe.forge.api.EchoWardrobeService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Query-only public service backed by runtime state.
 */
public final class ForgeWardrobeServices implements EchoWardrobeService {
    private final ForgeWardrobeRuntimeState state;

    ForgeWardrobeServices(ForgeWardrobeRuntimeState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    @Override
    public long catalogRevision() {
        return state.catalog().revision().value();
    }

    @Override
    public int wearableDefinitionCount() {
        return state.catalog().wearables().count();
    }

    @Override
    public List<WearableDefinition> wearableDefinitions() {
        return state.catalog().wearables().definitions();
    }

    @Override
    public Optional<WearableDefinition> wearable(WearableId id) {
        return state.catalog().wearables().find(id);
    }

    @Override
    public int outfitVariantCount() {
        return state.catalog().outfits().definitions().size();
    }

    @Override
    public List<OutfitVariantDefinition> outfitVariants() {
        return state.catalog().outfits().definitions();
    }

    @Override
    public Optional<OutfitVariantDefinition> outfitVariant(OutfitVariantId id) {
        return state.catalog().outfits().find(id);
    }

    @Override
    public Optional<OutfitVariantDefinition> exactOutfit(OutfitKey key) {
        return state.catalog().outfits().exact(key);
    }

    @Override
    public List<OutfitVariantDefinition> outfitVariants(WardrobeProfileId profileId) {
        return state.catalog().outfits().byProfile(profileId);
    }

    @Override
    public int wardrobeProfileCount() {
        return state.catalog().profiles().definitions().size();
    }

    @Override
    public List<WardrobeProfileDefinition> wardrobeProfiles() {
        return state.catalog().profiles().definitions();
    }

    @Override
    public Optional<WardrobeProfileDefinition> wardrobeProfile(WardrobeProfileId id) {
        return state.catalog().profiles().find(id);
    }

    @Override
    public Optional<EquippedWearableSnapshot> equippedWearables(UUID playerUuid) {
        return state.snapshot(playerUuid);
    }

    @Override
    public Optional<WardrobeAppearanceContribution> wardrobeAppearance(UUID playerUuid) {
        return state.appearance(playerUuid);
    }

    @Override
    public Optional<ResolvedOutfit> resolvedOutfit(UUID playerUuid) {
        return state.outfit(playerUuid);
    }

    @Override
    public Optional<OutfitKey> outfitKey(UUID playerUuid) {
        return state.outfit(playerUuid).map(ResolvedOutfit::outfitKey);
    }

    @Override
    public long appearanceRevision(UUID playerUuid) {
        return state.appearanceRevision(playerUuid);
    }

    @Override
    public List<WearableModelContribution> wearableModels(UUID playerUuid) {
        return state.outfit(playerUuid).map(ResolvedOutfit::models).orElse(List.of());
    }

    @Override
    public List<WardrobeDiagnostic> diagnostics() {
        return state.diagnostics();
    }
}
