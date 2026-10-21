package io.github.kxyr0.echonpc.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonpc.api.appearance.NpcNameColor;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeRequest;
import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.form.NpcFormCommandId;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyDimensions;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeRequest;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormTransition;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionFingerprint;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.rest.NpcRestAttachmentView;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.scene.NpcSceneCommandId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcServiceHealth;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NpcContractsTest {
    @Test
    void validNamespacedIdsAreCanonical() {
        assertEquals("sample:father", new ActorId("sample:father").value());
        assertEquals("sample:mutation-1", new ActorMutationId("sample:mutation-1").value());
        assertEquals("sample:father", new NpcDefinitionId("sample:father").value());
        assertEquals("sample:father", new NpcSkinId("sample:father").value());
        assertEquals("minecraft:overworld", new NpcWorldId("minecraft:overworld").value());
        assertEquals("jdh:sasha/child", new NpcFormId("jdh:sasha/child").value());
    }

    @Test
    void invalidNamespacedIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ActorId("missing_namespace"));
        assertThrows(IllegalArgumentException.class, () -> new NpcDefinitionId("sample:"));
        assertThrows(IllegalArgumentException.class, () -> new NpcSkinId("Sample:Father"));
        assertThrows(IllegalArgumentException.class, () -> new NpcWorldId("minecraft:/bad"));
        assertThrows(IllegalArgumentException.class, () -> new ActorMutationId("sample:" + "a".repeat(65)));
        assertThrows(IllegalArgumentException.class, () -> new NpcFormId("jdh:sasha child"));
    }

    @Test
    void spatialValuesRejectNonFiniteInput() {
        assertThrows(IllegalArgumentException.class, () -> new NpcPosition(Double.NaN, 64.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> new NpcRotation(0.0F, 120.0F));
    }

    @Test
    void publicModelsPreserveRussianDisplayNames() {
        var view = new NpcActorView(new ActorId("sample:john_father"), new NpcDefinitionId("sample:father"),
                "Отец", Optional.of(NpcNameColor.parse("#8FD8FF")),
                Optional.of(new NpcSkinId("sample:father")), new NpcWorldId("minecraft:overworld"),
                new NpcPosition(1.0D, 64.0D, 2.0D), NpcRotation.zero(), NpcActorView.Status.ACTIVE, 1L);
        assertEquals("Отец", view.displayName());
        assertEquals("#8FD8FF", view.nameColor().orElseThrow().hex());
    }

    @Test
    void nameColorsAcceptRgbAndOpaqueRgbaHex() {
        assertEquals(0x8FD8FF, NpcNameColor.parse("#8fd8ff").rgb());
        assertEquals("#8FD8FF", NpcNameColor.parse("0x8FD8FF").hex());
        assertEquals("#9227CF", NpcNameColor.parse("#9227cfff").hex());
        assertThrows(IllegalArgumentException.class, () -> NpcNameColor.parse("#12345"));
        assertThrows(IllegalArgumentException.class, () -> NpcNameColor.parse("#8fd8ff80"));
        assertThrows(IllegalArgumentException.class, () -> NpcNameColor.rgb(0xFF8FD8FF));
    }

    @Test
    void publicRequestsRejectNullsAndFingerprintIsStableValue() {
        assertThrows(NullPointerException.class, () -> new SpawnNpcRequest(null, new NpcDefinitionId("sample:a"),
                new NpcWorldId("minecraft:overworld"), new NpcPosition(0.0D, 64.0D, 0.0D), NpcRotation.zero()));
        var fingerprint = new NpcDefinitionFingerprint("a".repeat(64));
        assertTrue(fingerprint.toString().matches("[a-f0-9]{64}"));
    }

    @Test
    void publicRequestsCarryOptionalMutationIdentity() {
        var mutationId = new ActorMutationId("echo_story_npc_adapter:aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var spawn = new SpawnNpcRequest(new ActorId("sample:john"), new NpcDefinitionId("sample:father"),
                new NpcWorldId("minecraft:overworld"), new NpcPosition(0.0D, 64.0D, 0.0D),
                NpcRotation.zero(), mutationId);
        var teleport = new TeleportNpcRequest(new ActorId("sample:john"), new NpcWorldId("minecraft:overworld"),
                new NpcPosition(1.0D, 64.0D, 0.0D), NpcRotation.zero(), mutationId);
        var skin = new NpcSkinChangeRequest(new ActorId("sample:john"),
                new NpcSkinId("sample:father/backpack"), mutationId);

        assertEquals(Optional.of(mutationId), spawn.mutationId());
        assertEquals(Optional.of(mutationId), teleport.mutationId());
        assertEquals(Optional.of(mutationId), skin.mutationId());
        assertEquals("sample:father/backpack", skin.skinId().value());
        assertThrows(NullPointerException.class, () -> new SpawnNpcRequest(new ActorId("sample:john"),
                new NpcDefinitionId("sample:father"), new NpcWorldId("minecraft:overworld"),
                new NpcPosition(0.0D, 64.0D, 0.0D), NpcRotation.zero(), (ActorMutationId) null));
    }

    @Test
    void blockIdsPositionsAndSleepRequestsAreNeutralValues() {
        assertEquals("minecraft:red_bed", new BlockId("minecraft:red_bed").value());
        assertEquals("1,64,2", new BlockPosition(1, 64, 2).toString());
        var request = new NpcSleepRequest(new ActorId("sample:sasha"), new NpcWorldId("minecraft:overworld"),
                new BlockPosition(1, 64, 2), new BlockId("minecraft:red_bed"));
        assertEquals("sample:sasha", request.npcId().value());
        assertThrows(IllegalArgumentException.class, () -> new BlockId("missing_namespace"));
        assertThrows(IllegalArgumentException.class, () -> new BlockPosition(0, 5000, 0));
    }

    @Test
    void formRequestsAndBodyDimensionsValidatePublicValues() {
        var commandId = new NpcFormCommandId("sample:form-command");
        var request = new NpcFormChangeRequest(new ActorId("sample:sasha"),
                new NpcFormId("sample:sasha/adult"), commandId);

        assertEquals(Optional.of(commandId), request.commandId());
        assertEquals(NpcFormTransition.INSTANT, request.transition());
        assertEquals(2L, new NpcFormRevision(2L).value());
        assertEquals(1.06D, new NpcBodyDimensions(0.42D, 1.22D, 1.06D).eyeHeight());
        assertThrows(IllegalArgumentException.class, () -> new NpcFormRevision(-1L));
        assertThrows(IllegalArgumentException.class, () -> new NpcBodyDimensions(0.6D, 1.2D, 1.5D));
    }

    @Test
    void actorViewCanExposeRestAttachmentForPredicates() {
        NpcRestAttachmentView rest = restAttachment();
        var view = new NpcActorView(rest.npcId(), new NpcDefinitionId("sample:sasha"), "Саша",
                Optional.empty(), Optional.empty(), rest.worldId(), rest.sleepAnchor(),
                new NpcRotation(rest.sleepYaw(), 0.0F), NpcActorView.Status.NPC_SLEEPING,
                NpcActorView.DesiredPresence.ACTIVE, NpcActorView.DefinitionCompatibility.COMPATIBLE,
                Optional.of(rest.sleepAnchor()), Optional.of(rest), 2L, 2L, Optional.empty());

        assertTrue(view.restAttachment().isPresent());
        assertEquals("echo_npc:vanilla_bed", view.restAttachment().orElseThrow().providerId());
    }

    @Test
    void defaultDespawnMutationOverloadPreservesCompatibility() {
        NpcService service = new NpcService() {
            @Override
            public NpcResults.Spawn spawn(SpawnNpcRequest request) {
                return new NpcResults.Spawn(NpcResults.SpawnStatus.REJECTED, Optional.empty(), "Не используется.");
            }

            @Override
            public NpcResults.Query query(ActorId actorId) {
                return new NpcResults.Query(NpcResults.QueryStatus.NOT_FOUND, Optional.empty(), "Не используется.");
            }

            @Override
            public NpcResults.ListActors listActors(int limit) {
                return new NpcResults.ListActors(java.util.List.of(), false);
            }

            @Override
            public NpcResults.Teleport teleport(TeleportNpcRequest request) {
                return new NpcResults.Teleport(NpcResults.TeleportStatus.REJECTED, Optional.empty(),
                        "Не используется.");
            }

            @Override
            public NpcResults.Despawn despawn(ActorId actorId) {
                return new NpcResults.Despawn(NpcResults.DespawnStatus.ALREADY_ABSENT, "Совместимый вызов.");
            }

            @Override
            public NpcServiceHealth health() {
                return new NpcServiceHealth(false, 0L, 0, 0, 0, 0, 0, false, false, false);
            }
        };

        var result = service.despawn(new ActorId("sample:john"), new ActorMutationId("sample:mutation"));
        assertEquals(NpcResults.DespawnStatus.ALREADY_ABSENT, result.status());
    }

    private NpcRestAttachmentView restAttachment() {
        return new NpcRestAttachmentView(new ActorId("sample:sasha"), "echo_npc:vanilla_bed",
                new NpcWorldId("minecraft:overworld"), new BlockPosition(1, 64, 2),
                new BlockId("minecraft:red_bed"), new BlockPosition(1, 64, 2),
                new NpcPosition(1.5D, 64.5625D, 2.0D), 180.0F, "NPC_SLEEPING", 1L,
                "facing=north", true, new NpcSceneCommandId("sample:scene_command"));
    }
}
