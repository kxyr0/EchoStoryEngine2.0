package io.github.kxyr0.echonpc.persistence.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionFingerprint;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.scene.NpcSceneCommandId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorPresence;
import io.github.kxyr0.echonpc.core.actor.EntityBindingId;
import io.github.kxyr0.echonpc.core.persistence.ActorRepositoryResult;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ActorFileRepositoryTest {
    @TempDir
    Path temp;

    @Test
    void createAndFreshRepositoryLoadSameActor() {
        ActorFileRepository repository = repository();
        ActorRepositoryResult.Save created = repository.create(desired("sample:john", 1L, 0L));
        assertEquals(ActorRepositoryResult.SaveStatus.CREATED, created.status());
        ActorRepositoryResult.Load loaded = new ActorFileRepository(temp).load(new ActorId("sample:john"));
        assertEquals(ActorRepositoryResult.LoadStatus.FOUND, loaded.status());
        assertEquals("sample:john", loaded.actor().orElseThrow().actorId().value());
        assertEquals(1L, loaded.actor().orElseThrow().repositoryRevision());
    }

    @Test
    void updateRequiresExpectedRepositoryRevision() {
        ActorFileRepository repository = repository();
        ActorDesiredState created = repository.create(desired("sample:john", 1L, 0L)).actor().orElseThrow();
        ActorDesiredState moved = created.withTarget(created.worldId(), new NpcPosition(2.0D, 64.0D, 0.0D),
                NpcRotation.zero(), 2L);
        assertEquals(ActorRepositoryResult.SaveStatus.REVISION_CONFLICT, repository.update(moved, 99L).status());
        assertEquals(ActorRepositoryResult.SaveStatus.UPDATED,
                repository.update(moved, created.repositoryRevision()).status());
    }

    @Test
    void corruptPrimaryLoadsValidBackup() throws Exception {
        ActorFileRepository repository = repository();
        ActorDesiredState created = repository.create(desired("sample:john", 1L, 0L)).actor().orElseThrow();
        ActorDesiredState moved = created.withTarget(created.worldId(), new NpcPosition(3.0D, 64.0D, 0.0D),
                NpcRotation.zero(), 2L);
        repository.update(moved, created.repositoryRevision());
        Files.writeString(layout().primary(new ActorId("sample:john")), "{", StandardCharsets.UTF_8);
        ActorRepositoryResult.Load loaded = repository.load(new ActorId("sample:john"));
        assertEquals(ActorRepositoryResult.LoadStatus.FOUND, loaded.status());
        assertTrue(loaded.message().contains("backup"));
    }

    @Test
    void tombstonePreventsActiveBackupResurrectionAndRemovalDeletesRecord() {
        ActorFileRepository repository = repository();
        ActorDesiredState created = repository.create(desired("sample:john", 1L, 0L)).actor().orElseThrow();
        ActorDesiredState absent = created.absent(3L);
        ActorRepositoryResult.Removal tombstone = repository.tombstone(absent, created.repositoryRevision());
        assertEquals(ActorRepositoryResult.RemovalStatus.TOMBSTONED, tombstone.status());
        ActorRepositoryResult.Load loaded = repository.load(new ActorId("sample:john"));
        assertEquals(ActorPresence.ABSENT, loaded.actor().orElseThrow().presence());
        ActorRepositoryResult.Removal removed = repository.remove(new ActorId("sample:john"),
                loaded.actor().orElseThrow().repositoryRevision());
        assertEquals(ActorRepositoryResult.RemovalStatus.REMOVED, removed.status());
        assertEquals(ActorRepositoryResult.LoadStatus.NOT_FOUND, repository.load(new ActorId("sample:john")).status());
    }

    @Test
    void deterministicFilenameUsesCanonicalActorId() {
        String first = layout().primary(new ActorId("sample:john")).getFileName().toString();
        String second = layout().primary(new ActorId(" sample:john ")).getFileName().toString();
        assertEquals(first, second);
        assertTrue(first.endsWith(".json"));
    }

    @Test
    void restAttachmentRoundTripsThroughActorSchemaV2() {
        ActorFileRepository repository = repository();
        ActorDesiredState sleeping = desired("sample:sasha", 1L, 0L)
                .withRestAttachment(rest("sample:sasha"), 2L);
        ActorDesiredState created = repository.create(sleeping).actor().orElseThrow();

        ActorRepositoryResult.Load loaded = new ActorFileRepository(temp).load(new ActorId("sample:sasha"));

        assertEquals(created.repositoryRevision(), loaded.actor().orElseThrow().repositoryRevision());
        assertEquals("echo_npc:vanilla_bed",
                loaded.actor().orElseThrow().restAttachment().orElseThrow().providerId());
        assertEquals(new BlockPosition(1, 64, 2),
                loaded.actor().orElseThrow().restAttachment().orElseThrow().canonicalBedPosition());
    }

    @Test
    void activeFormRoundTripsThroughActorSchemaV4() throws Exception {
        ActorFileRepository repository = repository();
        ActorDesiredState withForm = desired("sample:sasha", 1L, 0L).withForm(
                new NpcFormId("sample:sasha/child"), new NpcFormRevision(5L), "b".repeat(64), 2L);
        ActorDesiredState created = repository.create(withForm).actor().orElseThrow();

        ActorRepositoryResult.Load loaded = new ActorFileRepository(temp).load(new ActorId("sample:sasha"));
        String json = Files.readString(layout().primary(new ActorId("sample:sasha")), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"schemaVersion\": 4"));
        assertTrue(json.contains("\"activeForm\""));
        assertEquals(created.repositoryRevision(), loaded.actor().orElseThrow().repositoryRevision());
        assertEquals(new NpcFormId("sample:sasha/child"),
                loaded.actor().orElseThrow().activeFormId().orElseThrow());
        assertEquals(new NpcFormRevision(5L),
                loaded.actor().orElseThrow().activeFormRevision().orElseThrow());
        assertEquals("b".repeat(64), loaded.actor().orElseThrow().activeFormFingerprint().orElseThrow());
    }

    @Test
    void activeSkinRoundTripsThroughActorSchemaV4() throws Exception {
        ActorFileRepository repository = repository();
        ActorDesiredState changed = desired("sample:sasha", 1L, 0L)
                .withSkin(new NpcSkinId("jdh:npc/sasha/clothes"), 2L);

        repository.create(changed);
        ActorDesiredState loaded = new ActorFileRepository(temp).load(new ActorId("sample:sasha"))
                .actor().orElseThrow();
        String json = Files.readString(layout().primary(new ActorId("sample:sasha")), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"activeSkinId\": \"jdh:npc/sasha/clothes\""));
        assertEquals(new NpcSkinId("jdh:npc/sasha/clothes"), loaded.activeSkinId().orElseThrow());
    }

    private ActorFileRepository repository() {
        return new ActorFileRepository(temp, new ActorRepositoryLimits(10));
    }

    private ActorFileLayout layout() {
        return new ActorFileLayout(temp);
    }

    private ActorDesiredState desired(String actorId, long actorRevision, long repositoryRevision) {
        return new ActorDesiredState(new ActorId(actorId), new NpcDefinitionId("sample:father"),
                new NpcDefinitionFingerprint("a".repeat(64)), ActorPresence.ACTIVE,
                new NpcWorldId("minecraft:overworld"), new NpcPosition(1.0D, 64.0D, 1.0D),
                NpcRotation.zero(), new EntityBindingId("12345678-1234-1234-1234-123456789abc"),
                actorRevision, repositoryRevision, 1L, 1L);
    }

    private NpcRestAttachment rest(String actorId) {
        return new NpcRestAttachment(new ActorId(actorId), "echo_npc:vanilla_bed",
                new NpcWorldId("minecraft:overworld"), new BlockPosition(1, 64, 2),
                new BlockId("minecraft:red_bed"), new BlockPosition(1, 64, 2),
                new NpcPosition(1.5D, 64.5625D, 2.0D), 180.0F, "NPC_SLEEPING", 1L,
                "facing=north", true, new NpcSceneCommandId("sample:scene_command"));
    }
}
