package io.github.kxyr0.echotrigger.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.event.TriggerEventKind;
import io.github.kxyr0.echotrigger.api.expression.TriggerExpressionKind;
import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.model.TriggerCondition;
import io.github.kxyr0.echotrigger.api.model.TriggerExpression;
import io.github.kxyr0.echotrigger.api.policy.TriggerManualFirePolicy;
import io.github.kxyr0.echotrigger.api.policy.TriggerMode;
import io.github.kxyr0.echotrigger.api.policy.TriggerScope;
import io.github.kxyr0.echotrigger.api.status.TriggerStatus;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog;
import io.github.kxyr0.echotrigger.core.definition.TriggerSequenceStep;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VanillaTriggerRuntimeTest {
    private final TriggerOwnerId owner = new TriggerOwnerId("player:a");

    @Test
    void predicateWaitCompletesFromPredicateSnapshotOnly() {
        TriggerRuntimeHost host = host("HasItem", TriggerExpressionKind.PREDICATE,
                Map.of("target", "minecraft:diamond"));
        var started = host.start(TriggerId.parse("jdh:t2"), owner, new TriggerCommandId("cmd:start"));

        host.applyEvent(TriggerEvent.vanilla(owner, 1, "ItemAdded", Map.of("item", "minecraft:diamond")));
        assertEquals(TriggerStatus.WAITING, host.query(started.instanceId().orElseThrow()).orElseThrow().status());

        host.applyEvent(TriggerEvent.predicateTrue(owner, 2, "HasItem", Map.of("item", "minecraft:diamond")));
        assertEquals(TriggerStatus.COMPLETED, host.query(started.instanceId().orElseThrow()).orElseThrow().status());
    }

    @Test
    void itemAddedMatchesTagAndCountDelta() {
        TriggerRuntimeHost host = host("ItemAdded", TriggerExpressionKind.EVENT,
                Map.of("target", "#minecraft:wool", "count", "3"));
        var started = host.start(TriggerId.parse("jdh:t2"), owner, new TriggerCommandId("cmd:start"));

        host.applyEvent(TriggerEvent.vanilla(owner, 1, "ItemAdded", Map.of(
                "item", "minecraft:white_wool", "tags", "#minecraft:wool", "count", "2")));
        assertEquals(TriggerStatus.WAITING, host.query(started.instanceId().orElseThrow()).orElseThrow().status());

        host.applyEvent(TriggerEvent.vanilla(owner, 2, "ItemAdded", Map.of(
                "item", "minecraft:white_wool", "tags", "#minecraft:wool", "count", "3")));
        assertEquals(TriggerStatus.COMPLETED, host.query(started.instanceId().orElseThrow()).orElseThrow().status());
    }

    private static TriggerRuntimeHost host(
            String expressionName,
            TriggerExpressionKind expressionKind,
            Map<String, String> arguments
    ) {
        TriggerId id = TriggerId.parse("jdh:t2");
        TriggerEventKind eventKind = expressionKind == TriggerExpressionKind.PREDICATE
                ? TriggerEventKind.PREDICATE_TRUE
                : TriggerEventKind.VANILLA_GAMEPLAY;
        TriggerCondition condition = TriggerCondition.expression(expressionKind, eventKind, Optional.empty(),
                expressionName, arguments);
        CompiledTriggerDefinition definition = new CompiledTriggerDefinition(id, TriggerScope.PLAYER,
                TriggerMode.ONCE, new TriggerRevision(1), 0, TriggerManualFirePolicy.DENY,
                List.of(new TriggerSequenceStep(0, new TriggerExpression(expressionName, condition))), "test");
        return new TriggerRuntimeHost(new TriggerDefinitionCatalog(List.of(definition)));
    }
}
