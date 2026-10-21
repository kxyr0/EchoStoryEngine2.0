package io.github.kxyr0.echotrigger.core.runtime;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.event.TriggerEventKind;
import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.model.TriggerArea;
import io.github.kxyr0.echotrigger.api.model.TriggerCondition;
import io.github.kxyr0.echotrigger.api.model.TriggerExpression;
import io.github.kxyr0.echotrigger.api.policy.TriggerManualFirePolicy;
import io.github.kxyr0.echotrigger.api.policy.TriggerMode;
import io.github.kxyr0.echotrigger.api.policy.TriggerScope;
import io.github.kxyr0.echotrigger.api.status.TriggerResultCode;
import io.github.kxyr0.echotrigger.api.status.TriggerStatus;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog;
import io.github.kxyr0.echotrigger.core.definition.TriggerSequenceStep;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TriggerRuntimeHostTest {
    private final TriggerId id = TriggerId.parse("jdh:leave_house");
    private final TriggerOwnerId owner = new TriggerOwnerId("player:a");
    private final TriggerOwnerId otherOwner = new TriggerOwnerId("player:b");
    private final TriggerArea hall = TriggerArea.block("minecraft:overworld", 1, 1, 1);
    private final TriggerArea exit = TriggerArea.block("minecraft:overworld", 2, 1, 1);

    @Test
    void startIsIdempotentForSameCommandAndDoesNotDuplicateInstance() {
        TriggerRuntimeHost host = host();
        TriggerCommandId commandId = new TriggerCommandId("cmd:start");

        var first = host.start(id, owner, commandId);
        var retry = host.start(id, owner, commandId);

        assertEquals(TriggerResultCode.STARTED, first.code());
        assertEquals(first.instanceId(), retry.instanceId());
        assertEquals(0, host.duplicateCount(id, owner));
    }

    @Test
    void stoppedInstanceCanBeStartedAgain() {
        TriggerRuntimeHost host = host();
        var first = host.start(id, owner, new TriggerCommandId("cmd:start"));

        host.stop(first.instanceId().orElseThrow(), new TriggerCommandId("cmd:stop"));
        var restarted = host.start(id, owner, new TriggerCommandId("cmd:restart"));

        assertEquals(TriggerResultCode.STARTED, restarted.code());
        assertEquals(TriggerStatus.WAITING, host.query(restarted.instanceId().orElseThrow()).orElseThrow().status());
        assertEquals(0, host.duplicateCount(id, owner));
    }

    @Test
    void clearRuntimeRemovesInstancesAndClearsCommandLedger() {
        TriggerRuntimeHost host = host();
        TriggerCommandId command = new TriggerCommandId("cmd:start");
        var first = host.start(id, owner, command);

        int removed = host.clearRuntime(owner);

        assertEquals(1, removed);
        assertEquals(Optional.empty(), host.query(first.instanceId().orElseThrow()));
        var restartedWithSameCommand = host.start(id, owner, command);
        assertEquals(TriggerResultCode.STARTED, restartedWithSameCommand.code());
        assertEquals(TriggerStatus.WAITING,
                host.query(restartedWithSameCommand.instanceId().orElseThrow()).orElseThrow().status());
    }

    @Test
    void clearRuntimeByTriggerAndOwnerKeepsOtherOwners() {
        TriggerRuntimeHost host = host();
        var first = host.start(id, owner, new TriggerCommandId("cmd:start:a"));
        var other = host.start(id, otherOwner, new TriggerCommandId("cmd:start:b"));

        int removed = host.clearRuntime(id, owner);

        assertEquals(1, removed);
        assertEquals(Optional.empty(), host.query(first.instanceId().orElseThrow()));
        assertEquals(TriggerStatus.WAITING, host.query(other.instanceId().orElseThrow()).orElseThrow().status());
    }

    @Test
    void sequenceRequiresCorrectOrder() {
        TriggerRuntimeHost host = host();
        var started = host.start(id, owner, new TriggerCommandId("cmd:start"));

        host.applyEvent(event(exit));
        assertEquals(0, host.query(started.instanceId().orElseThrow()).orElseThrow().sequenceStep());

        host.applyEvent(event(hall));
        host.applyEvent(event(exit));

        var completed = host.query(started.instanceId().orElseThrow()).orElseThrow();
        assertEquals(TriggerStatus.COMPLETED, completed.status());
        assertEquals(2, completed.sequenceStep());
    }

    private TriggerRuntimeHost host() {
        return new TriggerRuntimeHost(new TriggerDefinitionCatalog(List.of(definition())));
    }

    private CompiledTriggerDefinition definition() {
        return new CompiledTriggerDefinition(id, TriggerScope.PLAYER, TriggerMode.ONCE, new TriggerRevision(1),
                0, TriggerManualFirePolicy.DENY, List.of(step(0, hall), step(1, exit)), "test");
    }

    private static TriggerSequenceStep step(int index, TriggerArea area) {
        TriggerCondition condition = TriggerCondition.event(TriggerEventKind.PLAYER_ENTER, area);
        return new TriggerSequenceStep(index, new TriggerExpression("PlayerEnter", condition));
    }

    private TriggerEvent event(TriggerArea area) {
        return new TriggerEvent(TriggerEventKind.PLAYER_ENTER, Optional.of(area), owner, 1);
    }
}
