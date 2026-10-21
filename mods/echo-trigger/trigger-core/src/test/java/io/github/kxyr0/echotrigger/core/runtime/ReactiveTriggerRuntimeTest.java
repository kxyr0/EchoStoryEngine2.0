package io.github.kxyr0.echotrigger.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackDefinition;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackKind;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackSignature;
import io.github.kxyr0.echotrigger.api.geometry.BoxArea;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.policy.TriggerFirePolicy;
import io.github.kxyr0.echotrigger.api.policy.TriggerManualFirePolicy;
import io.github.kxyr0.echotrigger.api.policy.TriggerMode;
import io.github.kxyr0.echotrigger.api.policy.TriggerScope;
import io.github.kxyr0.echotrigger.api.policy.TriggerStayPolicy;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.ReactiveTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.TriggerBoundArea;
import io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog;
import io.github.kxyr0.echotrigger.core.runtime.reactive.TriggerReactiveRuntime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReactiveTriggerRuntimeTest {
    private static final String DIMENSION = "minecraft:overworld";
    private final TriggerId id = TriggerId.parse("jdh:reactive");
    private final TriggerOwnerId owner = new TriggerOwnerId("player:a");
    private final Vector3 outside = new Vector3(20, 65, 20);
    private final Vector3 inside = new Vector3(1, 65, 1);

    @Test
    void movementEnterPublishesCallbackInvocationAfterBaseline() {
        TriggerRuntimeHost host = host(callbacks(TriggerCallbackKind.ENTER), TriggerFirePolicy.repeat(0), 20);

        assertTrue(host.applyMovement(owner, DIMENSION, outside, outside, 1).isEmpty());
        var invocations = host.applyMovement(owner, DIMENSION, outside, inside, 2);

        assertEquals(1, invocations.size());
        assertEquals(TriggerCallbackKind.ENTER, invocations.get(0).kind());
        assertEquals("1,65,1", invocations.get(0).payload().get("position"));
    }

    @Test
    void stayUsesConfiguredInterval() {
        TriggerRuntimeHost host = host(callbacks(TriggerCallbackKind.ENTER, TriggerCallbackKind.STAY),
                TriggerFirePolicy.repeat(0), 5);

        host.applyMovement(owner, DIMENSION, outside, outside, 1);
        host.applyMovement(owner, DIMENSION, outside, inside, 2);
        assertTrue(host.applyMovement(owner, DIMENSION, inside, inside, 6).isEmpty());

        var invocations = host.applyMovement(owner, DIMENSION, inside, inside, 7);

        assertEquals(1, invocations.size());
        assertEquals(TriggerCallbackKind.STAY, invocations.get(0).kind());
    }

    @Test
    void dimensionChangePublishesExitForPreviousDimension() {
        TriggerRuntimeHost host = host(callbacks(TriggerCallbackKind.ENTER, TriggerCallbackKind.EXIT),
                TriggerFirePolicy.repeat(0), 20);

        host.applyMovement(owner, DIMENSION, outside, outside, 1);
        host.applyMovement(owner, DIMENSION, outside, inside, 2);
        var movement = new TriggerReactiveRuntime.Movement(owner, DIMENSION, "minecraft:the_nether",
                inside, inside, 3);

        var invocations = host.applyMovement(movement);

        assertEquals(1, invocations.size());
        assertEquals(TriggerCallbackKind.EXIT, invocations.get(0).kind());
    }

    @Test
    void fireOnlyOnceRejectsLaterCallbacksForSameOwnerAndDefinition() {
        TriggerFirePolicy policy = new TriggerFirePolicy(true, Set.of(), OptionalInt.empty(), 0);
        TriggerRuntimeHost host = host(callbacks(TriggerCallbackKind.ENTER, TriggerCallbackKind.EXIT), policy, 20);

        host.applyMovement(owner, DIMENSION, outside, outside, 1);
        assertEquals(1, host.applyMovement(owner, DIMENSION, outside, inside, 2).size());

        var movement = new TriggerReactiveRuntime.Movement(owner, DIMENSION, "minecraft:the_nether",
                inside, inside, 3);

        assertTrue(host.applyMovement(movement).isEmpty());
    }

    private TriggerRuntimeHost host(
            Map<TriggerCallbackKind, TriggerCallbackDefinition> callbacks,
            TriggerFirePolicy policy,
            long stayInterval
    ) {
        return new TriggerRuntimeHost(new TriggerDefinitionCatalog(List.of(definition(callbacks, policy,
                stayInterval))));
    }

    private CompiledTriggerDefinition definition(
            Map<TriggerCallbackKind, TriggerCallbackDefinition> callbacks,
            TriggerFirePolicy policy,
            long stayInterval
    ) {
        TriggerBoundArea area = new TriggerBoundArea("House", DIMENSION,
                new BoxArea(new Vector3(0, 64, 0), new Vector3(4, 68, 4)));
        ReactiveTriggerDefinition reactive = new ReactiveTriggerDefinition(area, callbacks, policy, stayInterval,
                TriggerStayPolicy.SKIP_WHILE_RUNNING);
        return new CompiledTriggerDefinition(id, TriggerScope.PLAYER, TriggerMode.REPEAT,
                new TriggerRevision(1), 0, TriggerManualFirePolicy.DENY, List.of(), Optional.of(reactive), "test");
    }

    private Map<TriggerCallbackKind, TriggerCallbackDefinition> callbacks(TriggerCallbackKind... kinds) {
        java.util.LinkedHashMap<TriggerCallbackKind, TriggerCallbackDefinition> callbacks =
                new java.util.LinkedHashMap<>();
        for (TriggerCallbackKind kind : kinds) {
            callbacks.put(kind, callback(kind));
        }
        return Map.copyOf(callbacks);
    }

    private TriggerCallbackDefinition callback(TriggerCallbackKind kind) {
        return new TriggerCallbackDefinition(kind, kind.methodName(), TriggerCallbackSignature.NONE, List.of());
    }
}
