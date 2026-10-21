package io.github.kxyr0.echonpcinteraction.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionProviderId;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionConsequenceKind;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionOutcome;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionReceipt;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import io.github.kxyr0.echonpcinteraction.api.result.NpcRideHandoffStrategy;
import io.github.kxyr0.echonpcinteraction.core.command.DeterministicInteractionIds;
import io.github.kxyr0.echonpcinteraction.core.provider.NpcInteractionProviderSelector;
import io.github.kxyr0.echonpcinteraction.core.state.NpcInteractionStateMachine;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class NpcInteractionCoreTest {
    @Test
    void providerSelectionUsesSpecificity() {
        NpcInteractionProviderSelector selector = new NpcInteractionProviderSelector();
        var selected = selector.select(List.of(candidate("low", 10), candidate("high", 40)));
        assertTrue(selected.selected());
        assertEquals("test:high", selected.providerId().orElseThrow().value());
    }

    @Test
    void providerSelectionDetectsConflict() {
        NpcInteractionProviderSelector selector = new NpcInteractionProviderSelector();
        var selected = selector.select(List.of(candidate("a", 20), candidate("b", 20)));
        assertFalse(selected.selected());
        assertEquals(NpcInteractionFailure.NPC_INTERACTION_PROVIDER_CONFLICT, selected.failure().orElseThrow());
    }

    @Test
    void stateMachineKeepsTerminalStatesTerminal() {
        NpcInteractionStateMachine machine = new NpcInteractionStateMachine();
        assertTrue(machine.canMove(NpcInteractionStatus.CREATED,
                NpcInteractionStatus.WAITING_FOR_MATERIALIZATION));
        assertFalse(machine.canMove(NpcInteractionStatus.COMPLETED, NpcInteractionStatus.RECOVERING));
    }

    @Test
    void receiptV2KeepsConsequenceEvidence() {
        var receipt = new NpcInteractionReceipt(new NpcInteractionCommandId("test:cmd"),
                new ActorId("test:npc"), new NpcInteractionProviderId("test:provider"),
                new BlockPosition(1, 2, 3), new BlockId("minecraft:oak_chair"), "before",
                "SUCCESS", "after", Optional.empty(), 10L, "NPC_SEATED", NpcInteractionOutcome.COMPLETED,
                List.of(NpcInteractionConsequenceKind.PROXY_STARTED_RIDING), Optional.of("spawned"),
                Optional.empty(), Optional.of("vehicle"), Optional.of(NpcRideHandoffStrategy.ECHO_SEAT_ANCHOR),
                Optional.of("anchor"), Optional.of("seat"), true, true, true, 2L, 2L);
        assertEquals(List.of(NpcInteractionConsequenceKind.PROXY_STARTED_RIDING), receipt.consequenceKinds());
        assertEquals(NpcRideHandoffStrategy.ECHO_SEAT_ANCHOR, receipt.rideHandoffStrategy().orElseThrow());
    }

    @Test
    void deterministicIdsStayShortForRecoveredDialogueOwner() {
        String ownerExecution = "story-action/jdh:manual_first_morning_msaokx62/load_dialogue:"
                + "echo_npc_interaction_interact_wait_key_".repeat(6);
        var actorId = new ActorId("jdh:mother");
        var position = new BlockPosition(37, -3, 103);
        var command = DeterministicInteractionIds.command(ownerExecution, actorId, position);
        var execution = DeterministicInteractionIds.execution(ownerExecution);
        var owner = DeterministicInteractionIds.owner(ownerExecution);
        assertTrue(command.value().startsWith("echo_npc_interaction:cmd_"));
        assertTrue(command.value().length() <= 160);
        assertTrue(execution.value().startsWith("echo_npc_interaction:exec_"));
        assertTrue(execution.value().length() <= 160);
        assertTrue(owner.value().startsWith("echo_npc_interaction:owner_"));
        assertTrue(owner.value().length() <= 160);
    }

    private NpcInteractionProviderSelector.Candidate candidate(String name, int specificity) {
        return new NpcInteractionProviderSelector.Candidate(
                new NpcInteractionProviderId("test:" + name), 0, specificity, true);
    }
}
