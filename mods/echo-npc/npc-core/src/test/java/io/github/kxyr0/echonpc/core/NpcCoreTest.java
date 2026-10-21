package io.github.kxyr0.echonpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonpc.api.appearance.NpcNameColor;
import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyAnchorDefinition;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyAnchorId;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyDimensions;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyGeometry;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyPartId;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyPartTransform;
import io.github.kxyr0.echonpc.api.form.definition.NpcAppearanceDefinition;
import io.github.kxyr0.echonpc.api.form.definition.NpcAppearanceRevision;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.scene.NpcSceneCommandId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorPresence;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.EntityBindingId;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.definition.NpcDefinitionCatalog;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.platform.PlatformActorHandle;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NpcCoreTest {
    @Test
    void catalogAndDefinitionAreImmutableAndRejectDuplicates() {
        NpcDefinition father = fatherDefinition("Отец");
        var catalog = NpcDefinitionCatalog.of(1L, List.of(father));
        assertEquals(father, catalog.find(new NpcDefinitionId("sample:father")).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> catalog.definitions().clear());
        assertThrows(IllegalArgumentException.class, () -> NpcDefinitionCatalog.of(2L, List.of(father, father)));
    }

    @Test
    void invalidDisplayNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> NpcDefinition.humanoid(new NpcDefinitionId("sample:x"),
                "", Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> NpcDefinition.humanoid(new NpcDefinitionId("sample:x"),
                "я".repeat(65), Optional.empty()));
    }

    @Test
    void semanticFingerprintIgnoresSourceAndCosmeticColor() {
        NpcDefinition father = fatherDefinition("Отец");
        NpcDefinition same = fatherDefinition("Отец");
        NpcDefinition renamed = fatherDefinition("Отец 2");
        NpcDefinition recolored = coloredFatherDefinition("Отец");
        NpcDefinition noSkin = NpcDefinition.humanoid(new NpcDefinitionId("sample:father"), "Отец",
                Optional.empty());
        assertEquals(father.fingerprint(), same.fingerprint());
        assertEquals(father.fingerprint(), recolored.fingerprint());
        assertNotEquals(father.fingerprint(), renamed.fingerprint());
        assertNotEquals(father.fingerprint(), noSkin.fingerprint());
    }

    @Test
    void actorRuntimeStoresDesiredStateAndMaterializationSeparately() {
        ActorRuntime runtime = new ActorRuntime(diagnostic -> { });
        runtime.start();
        NpcDefinition definition = fatherDefinition("Отец");
        ActorDesiredState desired = desired("sample:john", definition, 1L, 1L);
        runtime.install(desired, Optional.of(definition), NpcActorView.Status.PENDING_SPAWN, Optional.empty());
        assertEquals(NpcResults.QueryStatus.FOUND, runtime.query(new ActorId("sample:john")).status());
        runtime.markMaterialized(new ActorId("sample:john"), new PlatformActorHandle("entity-1"));
        var materialized = runtime.query(new ActorId("sample:john")).actor().orElseThrow();
        assertEquals(NpcActorView.Status.MATERIALIZED, materialized.status());
        assertTrue(materialized.materializedPosition().isPresent());
        ActorDesiredState moved = desired.withTarget(new NpcWorldId("minecraft:overworld"),
                new NpcPosition(4.0D, 64.0D, 0.0D), NpcRotation.zero(), 2L).withRepositoryRevision(2L);
        runtime.updateDesired(moved, Optional.of(definition), NpcActorView.Status.PENDING_MOVE, Optional.empty());
        assertEquals(2L, runtime.query(new ActorId("sample:john")).actor().orElseThrow().actorRevision());
        runtime.remove(new ActorId("sample:john"));
        assertEquals(NpcResults.QueryStatus.NOT_FOUND, runtime.query(new ActorId("sample:john")).status());
    }

    @Test
    void actorViewCarriesDefinitionNameColor() {
        ActorRuntime runtime = new ActorRuntime(diagnostic -> { });
        runtime.start();
        NpcDefinition definition = coloredFatherDefinition("Отец");

        runtime.install(desired("sample:john", definition, 1L, 1L), Optional.of(definition),
                NpcActorView.Status.PENDING_SPAWN, Optional.empty());

        assertEquals("#8FD8FF", runtime.query(new ActorId("sample:john")).actor().orElseThrow()
                .nameColor().orElseThrow().hex());
    }

    @Test
    void catalogAndActorViewsExposeStableActiveForms() {
        ActorRuntime runtime = new ActorRuntime(diagnostic -> { });
        runtime.start();
        NpcFormDefinition child = form("sample:sasha/child", 7L, 0.74D);
        NpcFormDefinition adult = form("sample:sasha/adult", 8L, 1.0D);
        NpcDefinition sasha = NpcDefinition.humanoid(new NpcDefinitionId("sample:sasha"), "Саша",
                Optional.empty(), Optional.of(new NpcSkinId("sample:sasha")),
                Optional.of(child.id()));
        var catalog = NpcDefinitionCatalog.of(4L, List.of(sasha), List.of(adult, child));

        assertEquals(child, catalog.findForm(child.id()).orElseThrow());
        assertEquals(child.id(), catalog.summaries(10).get(0).defaultFormId().orElseThrow());
        ActorDesiredState desired = desired("sample:sasha", sasha, 1L, 1L).withInitialForm(child);
        runtime.install(desired, Optional.of(sasha), NpcActorView.Status.PENDING_SPAWN, Optional.empty());
        var view = runtime.query(new ActorId("sample:sasha")).actor().orElseThrow();

        assertEquals(new ActorId("sample:sasha"), view.actorId());
        assertEquals(child.id(), view.activeFormId().orElseThrow());
        assertEquals(child.revision(), view.activeFormRevision().orElseThrow());
        assertEquals(child.fingerprint(), view.activeFormFingerprint().orElseThrow());
        ActorDesiredState changed = desired.withForm(adult, 2L);
        assertEquals(adult.id(), changed.activeFormId().orElseThrow());
        assertEquals(2L, changed.actorRevision());
    }

    @Test
    void restAttachmentMarksActorSleepingAndOccupiesCanonicalBed() {
        ActorRuntime runtime = new ActorRuntime(diagnostic -> { });
        runtime.start();
        NpcDefinition definition = fatherDefinition("Саша");
        ActorDesiredState desired = desired("sample:sasha", definition, 1L, 1L)
                .withRestAttachment(rest("sample:sasha"), 2L);

        runtime.install(desired, Optional.of(definition), NpcActorView.Status.PENDING_MOVE, Optional.empty());
        runtime.markMaterialized(new ActorId("sample:sasha"), new PlatformActorHandle("entity-1"));

        var actor = runtime.query(new ActorId("sample:sasha")).actor().orElseThrow();
        assertEquals(NpcActorView.Status.NPC_SLEEPING, actor.status());
        assertTrue(actor.restAttachment().isPresent());
        assertTrue(runtime.restOccupied(new BlockPosition(1, 64, 2), new ActorId("sample:other")));
    }

    @Test
    void stoppedRuntimeRejectsReadAndAbsentActorIsNotListed() {
        ActorRuntime runtime = new ActorRuntime(diagnostic -> { });
        runtime.start();
        ActorDesiredState absent = desired("sample:absent", fatherDefinition("Отец"), 1L, 1L).absent(2L);
        runtime.install(absent, Optional.empty(), NpcActorView.Status.PENDING_REMOVAL, Optional.empty());
        assertTrue(runtime.listActors(10).actors().isEmpty());
        runtime.stop();
        assertEquals(NpcResults.QueryStatus.SERVICE_STOPPED, runtime.query(new ActorId("sample:john")).status());
    }

    @Test
    void blockedActorsRemainVisibleAndActorIdNeverComesFromPlatformHandle() {
        java.util.ArrayList<NpcDiagnostic> diagnostics = new java.util.ArrayList<>();
        ActorRuntime runtime = new ActorRuntime(diagnostics::add);
        runtime.start();
        NpcDefinition definition = fatherDefinition("Отец");
        runtime.install(desired("sample:john", definition, 1L, 1L), Optional.empty(),
                NpcActorView.Status.BLOCKED_DEFINITION_MISSING, Optional.of("missing"));
        var actor = runtime.query(new ActorId("sample:john")).actor().orElseThrow();
        assertEquals(NpcActorView.Status.BLOCKED_DEFINITION_MISSING, actor.status());
        assertTrue(actor.blockedReason().isPresent());
        runtime.reportMissing(new ActorId("sample:john"));
        assertTrue(diagnostics.stream().anyMatch(item -> item.code().equals("ENPC-ACTOR-ENTITY-MISSING")));
    }

    private NpcDefinition fatherDefinition(String displayName) {
        return NpcDefinition.humanoid(new NpcDefinitionId("sample:father"), displayName,
                Optional.of(new NpcSkinId("sample:father")));
    }

    private NpcDefinition coloredFatherDefinition(String displayName) {
        return NpcDefinition.humanoid(new NpcDefinitionId("sample:father"), displayName,
                Optional.of(NpcNameColor.parse("#8FD8FF")), Optional.of(new NpcSkinId("sample:father")));
    }

    private NpcFormDefinition form(String id, long revision, double scale) {
        var appearance = new NpcAppearanceDefinition(Optional.of(new NpcSkinId("sample:sasha")),
                "echo_npc:humanoid", "sample:sasha/walk", "sample:sasha/face",
                "sample:sasha/portrait", new NpcAppearanceRevision(revision));
        var geometry = new NpcBodyGeometry(scale, List.of(
                NpcBodyPartTransform.uniform(NpcBodyPartId.HEAD, scale)));
        var dimensions = new NpcBodyDimensions(0.42D * scale, 1.22D * scale, 1.06D * scale);
        var anchors = List.of(new NpcBodyAnchorDefinition(NpcBodyAnchorId.EYES, 0.0D,
                dimensions.eyeHeight(), 0.0D));
        return new NpcFormDefinition(new NpcFormId(id), appearance, geometry, dimensions, anchors,
                Set.of("sasha"), new NpcFormRevision(revision), id + ".form.es", "b".repeat(64));
    }

    private ActorDesiredState desired(String actorId, NpcDefinition definition, long actorRevision,
            long repositoryRevision) {
        return new ActorDesiredState(new ActorId(actorId), definition.id(), definition.fingerprint(),
                ActorPresence.ACTIVE, new NpcWorldId("minecraft:overworld"),
                new NpcPosition(0.0D, 64.0D, 0.0D), NpcRotation.zero(),
                new EntityBindingId("12345678-1234-1234-1234-123456789abc"), actorRevision,
                repositoryRevision, 1L, 1L);
    }

    private NpcRestAttachment rest(String actorId) {
        return new NpcRestAttachment(new ActorId(actorId), "echo_npc:vanilla_bed",
                new NpcWorldId("minecraft:overworld"), new BlockPosition(1, 64, 2),
                new BlockId("minecraft:red_bed"), new BlockPosition(1, 64, 2),
                new NpcPosition(1.5D, 64.5625D, 2.0D), 180.0F, "NPC_SLEEPING", 1L,
                "facing=north", true, new NpcSceneCommandId("sample:scene_command"));
    }
}
