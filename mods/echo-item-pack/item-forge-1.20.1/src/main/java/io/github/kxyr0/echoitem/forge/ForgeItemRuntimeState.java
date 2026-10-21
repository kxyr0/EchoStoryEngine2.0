package io.github.kxyr0.echoitem.forge;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.ItemKindId;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import io.github.kxyr0.echoitem.core.catalog.WeaponDefinitionCatalog;
import io.github.kxyr0.echoitem.core.manifest.ItemRegistrationPlan;
import io.github.kxyr0.echoitem.core.manifest.ItemRegistrationPlanner;
import io.github.kxyr0.echoitem.core.manifest.RegisteredItemManifestStore;
import io.github.kxyr0.echoitem.core.manifest.RegisteredItemRecord;
import io.github.kxyr0.echoitem.core.reload.ItemCandidateCompiler;
import io.github.kxyr0.echoitem.core.reload.ItemReloadOutcome;
import io.github.kxyr0.echoitem.core.reload.ItemReloadPlanner;
import io.github.kxyr0.echoitem.core.script.EchoScriptWeaponCompiler;
import io.github.kxyr0.echoitem.core.source.ExternalItemSource;
import io.github.kxyr0.echoitem.core.source.ItemSourceCandidate;
import io.github.kxyr0.echoitem.core.source.ItemSourceResolver;
import io.github.kxyr0.echoitem.core.source.PackagedItemSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Server-authoritative runtime state assembled from item-pack sources.
 */
public final class ForgeItemRuntimeState {
    private static final List<String> PACKAGED_WEAPONS = List.of(
            "data/jdh/echo_item/items/weapons/swords/void_sword.weapon.es",
            "data/jdh/echo_item/items/weapons/daggers/training_dagger.weapon.es"
    );
    private final WeaponDefinitionCatalog catalog = new WeaponDefinitionCatalog();
    private final ItemReloadPlanner reloadPlanner = new ItemReloadPlanner(
            new ItemCandidateCompiler(new EchoScriptWeaponCompiler()));
    private final ExternalItemSource externalSource;
    private final PackagedItemSource packagedSource;
    private final ItemSourceResolver sourceResolver = new ItemSourceResolver();
    private final RegisteredItemManifestStore manifestStore;
    private List<ItemDefinitionDiagnostic> diagnostics = List.of();
    private ItemRegistrationPlan registrationPlan = new ItemRegistrationPlan(List.of(), List.of(), List.of(),
            List.of());

    private ForgeItemRuntimeState(Path gameDir, ClassLoader classLoader) {
        this.externalSource = new ExternalItemSource(gameDir);
        this.packagedSource = new PackagedItemSource(classLoader);
        this.manifestStore = new RegisteredItemManifestStore(gameDir);
    }

    public static ForgeItemRuntimeState create(Path gameDir, ClassLoader classLoader) {
        ForgeItemRuntimeState state = new ForgeItemRuntimeState(gameDir, classLoader);
        state.startup();
        return state;
    }

    public WeaponDefinitionCatalog catalog() {
        return catalog;
    }

    public List<EchoItemId> registeredIds() {
        return registrationPlan.registeredIds();
    }

    public List<ItemDefinitionDiagnostic> diagnostics() {
        return diagnostics;
    }

    public ItemRegistrationPlan registrationPlan() {
        return registrationPlan;
    }

    public ItemReloadOutcome reload() {
        ItemReloadOutcome outcome = reloadPlanner.reload(catalog.snapshot(), readSources());
        diagnostics = outcome.diagnostics();
        if (outcome.applied()) {
            catalog.publish(outcome.snapshot());
        }
        refreshPending();
        writeManifest();
        return outcome;
    }

    public ItemReloadOutcome validateOnly() {
        ItemReloadPlanner planner = new ItemReloadPlanner(new ItemCandidateCompiler(new EchoScriptWeaponCompiler()));
        return planner.reload(ItemCatalogSnapshot.empty(), readSources());
    }

    private void startup() {
        ItemReloadOutcome outcome = reloadPlanner.reload(ItemCatalogSnapshot.empty(), readSources());
        diagnostics = outcome.diagnostics();
        if (outcome.applied()) {
            catalog.publish(outcome.snapshot());
            reloadPlanner.remember(readSources());
        }
        registrationPlan = new ItemRegistrationPlanner().startup(manifestStore.load(), catalog.snapshot().weaponIds());
        writeManifest();
    }

    private List<ItemSourceCandidate> readSources() {
        try {
            return sourceResolver.merge(packagedSource.weaponCandidates(PACKAGED_WEAPONS),
                    externalSource.weaponCandidates());
        } catch (IOException | RuntimeException ex) {
            diagnostics = List.of(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    ex.getMessage(), externalSource.root().toString()));
            return List.of();
        }
    }

    private void refreshPending() {
        registrationPlan = new ItemRegistrationPlanner().live(manifestStore.load(), registeredIds(),
                catalog.snapshot().weaponIds());
    }

    private void writeManifest() {
        try {
            var previous = manifestStore.load();
            List<RegisteredItemRecord> records = new ArrayList<>();
            for (EchoItemId id : registeredIds()) {
                WeaponDefinition definition = catalog.current(id);
                records.add(record(previous, id, ItemKindId.WEAPON, definition.sourceFingerprint(), false));
            }
            registrationPlan.pendingRestartIds().forEach(pending -> records.add(record(previous, pending.id(),
                    pending.kind(), pendingFingerprint(pending.id()), true)));
            manifestStore.write(new io.github.kxyr0.echoitem.core.manifest.RegisteredItemManifest(records));
        } catch (IOException ex) {
            diagnostics = List.of(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.MANIFEST_WRITE_FAILED,
                    ex.getMessage(), manifestStore.path().toString()));
        }
    }

    private RegisteredItemRecord record(io.github.kxyr0.echoitem.core.manifest.RegisteredItemManifest previous,
                                        EchoItemId id, ItemKindId kind, ItemSourceFingerprint fingerprint,
                                        boolean pending) {
        Optional<RegisteredItemRecord> existing = previous.find(id);
        String firstSeen = existing.map(RegisteredItemRecord::firstSeenVersion).orElse(EchoItemPackMod.MOD_VERSION);
        boolean deprecated = existing.map(RegisteredItemRecord::deprecated).orElse(false);
        return new RegisteredItemRecord(id, kind, firstSeen, fingerprint, pending, deprecated);
    }

    private ItemSourceFingerprint pendingFingerprint(EchoItemId id) {
        return catalog.snapshot().weapon(id)
                .map(WeaponDefinition::sourceFingerprint)
                .orElseGet(() -> new ItemSourceFingerprint("0".repeat(64)));
    }
}
