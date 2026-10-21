package io.github.kxyr0.echonpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorPresence;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.EntityBindingId;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ActorSkinStateTest {
    @Test
    void activeSkinOverridesDefinitionWithoutChangingActorIdentityOrForm() {
        ActorRuntime runtime = new ActorRuntime(diagnostic -> { });
        runtime.start();
        NpcDefinition definition = NpcDefinition.humanoid(
                new NpcDefinitionId("sample:sasha"), "Саша",
                Optional.of(new NpcSkinId("sample:sasha/default")));
        ActorDesiredState changed = desired(definition).withSkin(new NpcSkinId("sample:sasha/clothes"), 2L);

        runtime.install(changed, Optional.of(definition), NpcActorView.Status.MATERIALIZED, Optional.empty());
        var actor = runtime.query(new ActorId("sample:sasha")).actor().orElseThrow();

        assertEquals(new ActorId("sample:sasha"), actor.actorId());
        assertEquals(new NpcSkinId("sample:sasha/clothes"), actor.skinId().orElseThrow());
        assertTrue(actor.activeFormId().isEmpty());
        assertEquals(2L, actor.actorRevision());
    }

    private ActorDesiredState desired(NpcDefinition definition) {
        return new ActorDesiredState(new ActorId("sample:sasha"), definition.id(), definition.fingerprint(),
                ActorPresence.ACTIVE, new NpcWorldId("minecraft:overworld"),
                new NpcPosition(0.0D, 64.0D, 0.0D), NpcRotation.zero(),
                new EntityBindingId("12345678-1234-1234-1234-123456789abc"), 1L, 1L, 1L, 1L);
    }
}
