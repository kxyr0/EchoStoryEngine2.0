package io.github.kxyr0.echowardrobe.forge;

import io.github.kxyr0.echowardrobe.api.contribution.WardrobeAppearanceContribution;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.outfit.ResolvedOutfit;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.runtime.EquippedWearableSnapshot;
import io.github.kxyr0.echowardrobe.core.catalog.WardrobeCatalog;
import io.github.kxyr0.echowardrobe.core.equipment.EquipmentItemSnapshot;
import io.github.kxyr0.echowardrobe.core.reload.LastKnownGoodCatalog;
import io.github.kxyr0.echowardrobe.core.resolution.ExactOutfitResolver;
import io.github.kxyr0.echowardrobe.core.resolution.WearableItemResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runtime-only wardrobe state derived from current equipment and catalog publication.
 */
public final class ForgeWardrobeRuntimeState {
    public static final WardrobeProfileId DEFAULT_PROFILE = WardrobeProfileId.parse("jdh:john");
    public static final String DEFAULT_BASE_SKIN = "echo_content_player:player/base";

    private final ExactOutfitResolver resolver = new ExactOutfitResolver();
    private final WearableItemResolver itemResolver = new WearableItemResolver();
    private final Map<UUID, EquipmentItemSnapshot> equipment = new ConcurrentHashMap<>();
    private final Map<UUID, EquippedWearableSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<UUID, WardrobeAppearanceContribution> appearances = new ConcurrentHashMap<>();
    private final Map<UUID, ResolvedOutfit> outfits = new ConcurrentHashMap<>();
    private final ForgeWardrobeServices services = new ForgeWardrobeServices(this);
    private final LastKnownGoodCatalog lastKnownGood = new LastKnownGoodCatalog();
    private final AtomicLong appearanceRevision = new AtomicLong();
    private volatile WardrobeCatalog catalog = WardrobeCatalog.empty();
    private volatile List<WardrobeDiagnostic> diagnostics = List.of();

    public ForgeWardrobeServices services() {
        return services;
    }

    public WardrobeCatalog catalog() {
        return catalog;
    }

    public void publishCatalog(WardrobeCatalog nextCatalog) {
        catalog = nextCatalog;
        equipment.forEach(this::resolveAppearance);
    }

    public WardrobeResult<WardrobeCatalog> acceptCatalog(WardrobeResult<WardrobeCatalog> candidate, String source) {
        WardrobeResult<WardrobeCatalog> accepted = lastKnownGood.accept(candidate, source);
        accepted.value().ifPresent(this::publishCatalog);
        diagnostics = accepted.diagnostics();
        return accepted;
    }

    public void observe(UUID playerUuid, EquipmentItemSnapshot equipment) {
        this.equipment.put(playerUuid, equipment);
        resolveAppearance(playerUuid, equipment);
    }

    private void resolveAppearance(UUID playerUuid, EquipmentItemSnapshot equipment) {
        List<WardrobeDiagnostic> nextDiagnostics = new ArrayList<>();
        EquippedWearableSnapshot snapshot = itemResolver.resolve(DEFAULT_PROFILE, equipment, catalog.wearables(),
                catalog.revision().value(), nextDiagnostics);
        long revision = appearanceRevision.incrementAndGet();
        var result = resolver.resolve(DEFAULT_PROFILE, equipment, catalog, DEFAULT_BASE_SKIN, revision);
        nextDiagnostics.addAll(result.diagnostics());
        snapshots.put(playerUuid, snapshot);
        result.value().ifPresentOrElse(outfit -> {
            outfits.put(playerUuid, outfit);
            appearances.put(playerUuid, resolver.contribution(outfit));
        }, () -> {
            outfits.remove(playerUuid);
            appearances.remove(playerUuid);
        });
        diagnostics = List.copyOf(nextDiagnostics);
    }

    public Optional<EquippedWearableSnapshot> snapshot(UUID playerUuid) {
        return Optional.ofNullable(snapshots.get(playerUuid));
    }

    public Optional<WardrobeAppearanceContribution> appearance(UUID playerUuid) {
        return Optional.ofNullable(appearances.get(playerUuid));
    }

    public Optional<ResolvedOutfit> outfit(UUID playerUuid) {
        return Optional.ofNullable(outfits.get(playerUuid));
    }

    public long appearanceRevision(UUID playerUuid) {
        return outfit(playerUuid).map(ResolvedOutfit::appearanceRevision).orElse(0L);
    }

    public List<WardrobeDiagnostic> diagnostics() {
        return diagnostics;
    }
}
