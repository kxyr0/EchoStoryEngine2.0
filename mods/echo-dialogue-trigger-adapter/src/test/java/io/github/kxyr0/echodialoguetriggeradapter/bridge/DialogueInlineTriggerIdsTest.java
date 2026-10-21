package io.github.kxyr0.echodialoguetriggeradapter.bridge;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DialogueInlineTriggerIdsTest {
    @Test
    void inlineTriggerInstanceIdIsDeterministicFromDialogueCommandContext() {
        DialogueTriggerSessionKey key = new DialogueTriggerSessionKey(
                new DialogueSessionId("session-1"), "WaitForExit", 3, "PlayerEnter(Exit)");
        DialogueInlineTriggerIds ids = new DialogueInlineTriggerIds();

        assertEquals(ids.instanceId(key), ids.instanceId(key));
    }
}
